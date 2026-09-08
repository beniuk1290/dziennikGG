package pl.szczodrzynski.edziennik.data.api.edziennik.eduvulcan.login

import android.util.Base64
import org.json.JSONObject
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.utils.Utils.getOkHttp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class EduVulcanLoginPrometheus(
    private val data: DataVulcan,
    private val onSuccess: (tokens: List<String>, tenantTokens: Map<String, String>) -> Unit,
    private val onError: (Int, String?) -> Unit
) {
    companion object {
        private const val TAG = "EduVulcanLoginPrometheus"
        private const val BASE_URL = "https://eduvulcan.pl"
    }

    private val cookieJar = InMemoryCookieJar()
    private val client: OkHttpClient by lazy {
        getOkHttp()
            .cookieJar(cookieJar)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    fun login(email: String, password: String) {
        try {
            Timber.d("$TAG: Starting eduVULCAN login")

            queryUserInfo(email)

            val loginPageHtml = GET("$BASE_URL/logowanie")
            if (loginPageHtml.contains("Twój adres e-mail nie został jeszcze potwierdzony")) {
                onError(ERROR_LOGIN_VULCAN_INVALID_TOKEN, "Adres e-mail nie został potwierdzony")
                return
            }

            val csrfToken = extractCsrfToken(loginPageHtml)
            Timber.d("$TAG: Got CSRF token")

            val showCaptcha = queryUserInfo(email)
            val captchaResponse = if (showCaptcha) {
                val captchaParams = extractCaptchaParams(loginPageHtml)
                POWCaptchaResolver.computeCaptchaResponse(
                    captchaParams.first, captchaParams.second, captchaParams.third
                )
            } else ""

            val formBody = buildString {
                append("UserName=").append(URLEncoder.encode(email, "UTF-8"))
                append("&Password=").append(URLEncoder.encode(password, "UTF-8"))
                append("&captcha-response=").append(URLEncoder.encode(captchaResponse, "UTF-8"))
                append("&__RequestVerificationToken=").append(URLEncoder.encode(csrfToken, "UTF-8"))
            }

            val loginResponse = POST("$BASE_URL/logowanie", formBody)
            if (loginResponse.contains("Twój adres e-mail nie został jeszcze potwierdzony")) {
                onError(ERROR_LOGIN_VULCAN_INVALID_TOKEN, "Adres e-mail nie został potwierdzony")
                return
            }

            val locationHeader = getLastRedirect()
            if (locationHeader == null) {
                onError(ERROR_LOGIN_VULCAN_INVALID_TOKEN, "Nieprawidłowe dane logowania")
                return
            }
            Timber.d("$TAG: Login successful, got redirect")

            val apiApResponse = processApiAp(null)
            if (apiApResponse == null) {
                onError(ERROR_LOGIN_VULCAN_INVALID_TOKEN, "Nie udało się pobrać tokenów")
                return
            }

            val tokens = apiApResponse.getJSONArray("tokens")
            val tokenList = mutableListOf<String>()
            val tenantTokens = mutableMapOf<String, String>()

            for (i in 0 until tokens.length()) {
                val jwt = tokens.getString(i)
                tokenList.add(jwt)
                val tenant = decodeJWT(jwt)
                if (tenant != null && !tenantTokens.containsKey(tenant)) {
                    tenantTokens[tenant] = jwt
                }
            }

            if (tokenList.isEmpty()) {
                onError(ERROR_LOGIN_VULCAN_INVALID_TOKEN, "Brak uczniów na koncie")
                return
            }

            Timber.d("$TAG: Got ${tokenList.size} tokens, ${tenantTokens.size} tenants")
            onSuccess(tokenList, tenantTokens)
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Login failed")
            onError(ERROR_LOGIN_VULCAN_INVALID_TOKEN, e.message)
        }
    }

    private fun queryUserInfo(username: String): Boolean {
        return try {
            val body = POST_FORM(
                "$BASE_URL/Account/QueryUserInfo",
                "UserName=${URLEncoder.encode(username, "UTF-8")}"
            )
            val json = JSONObject(body)
            json.optJSONObject("data")?.optBoolean("ShowCaptcha") ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun processApiAp(accessToken: String?): JSONObject? {
        return try {
            val html = GET_WITH_HEADERS("$BASE_URL/api/ap", accessToken)
            val doc = org.jsoup.Jsoup.parse(html)
            val rawValue = doc.selectFirst("#ap")?.attr("value") ?: return null
            JSONObject(rawValue)
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to get /api/ap")
            null
        }
    }

    private fun extractCsrfToken(html: String): String {
        val doc = org.jsoup.Jsoup.parse(html)
        return doc.selectFirst("input[name=__RequestVerificationToken]")
            ?.attr("value")
            ?: throw IllegalStateException("CSRF token not found")
    }

    private fun extractCaptchaParams(html: String): Triple<String, Long, Int> {
        val doc = org.jsoup.Jsoup.parse(html)
        val wrapper = doc.selectFirst("div.captcha-wrapper")
            ?: throw IllegalStateException("captcha-wrapper not found")
        val challenge = wrapper.attr("data-challenge")
        val difficulty = wrapper.attr("data-difficulty").toLong()
        val rounds = wrapper.attr("data-rounds").toInt()
        return Triple(challenge, difficulty, rounds)
    }

    private fun decodeJWT(jwt: String): String? {
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val json = JSONObject(String(decoded))
            json.optString("tenant", null)
        } catch (e: Exception) {
            null
        }
    }

    private fun GET(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .get()
            .build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    private fun GET_WITH_HEADERS(url: String, accessToken: String?): String {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "Dart/3.11 (dart:io)")
            .header("vapi", "1")
            .header("vcanonicalurl", "api%2fap")
            .header("vos", "Android")
            .header("vversioncode", "998")
            .header("vdate", java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US).format(java.util.Date()))
        if (accessToken != null) {
            builder.header("Authorization", "Bearer $accessToken")
        }
        val response = client.newCall(builder.get().build()).execute()
        return response.body?.string() ?: ""
    }

    private fun POST(url: String, body: String): String {
        val requestBody = body.toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .post(requestBody)
            .build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    private fun POST_FORM(url: String, body: String): String {
        val requestBody = body.toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .post(requestBody)
            .build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    private fun getLastRedirect(): String? {
        return cookieJar.getAll().firstOrNull { it.name == ".AspNetCore.Identity.Application" }?.let { "ok" }
    }

    private val ERROR_LOGIN_VULCAN_INVALID_TOKEN = 310
}

class InMemoryCookieJar : CookieJar {
    private val cookies = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, List: List<Cookie>) {
        val key = url.host
        val existing = cookies.getOrPut(key) { mutableListOf() }
        existing.removeAll { cookie -> List.any { it.name == cookie.name } }
        existing.addAll(List)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies[url.host] ?: emptyList()
    }

    fun getAll(): List<Cookie> {
        return cookies.values.flatten()
    }

    fun clear(host: String) {
        cookies.remove(host)
    }
}
