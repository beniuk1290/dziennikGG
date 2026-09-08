package io.github.szpontium.update

import kotlinx.serialization.Serializable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val assets: List<Asset>,
    val published_at: String
) {
    @Serializable
    data class Asset(
        val name: String,
        val browser_download_url: String
    )
}

data class UpdateInfo(
    val version: String,
    val apkUrl: String,
    val publishedAt: String
)

class UpdateManager(private val httpClient: HttpClient) {
    private val _updateAvailable = MutableStateFlow<UpdateInfo?>(null)
    val updateAvailable: StateFlow<UpdateInfo?> = _updateAvailable

    private val json = Json { ignoreUnknownKeys = true }
    
    // Wersja aplikacji (musi być taka sama jak w build.gradle.kts)
    companion object {
        const val CURRENT_VERSION = "0.67.0"
        const val GITHUB_REPO = "beniuk1290/dziennikGG"
        const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    }

    suspend fun checkForUpdates(): UpdateInfo? {
        return try {
            val response = httpClient.get(GITHUB_API_URL)
            val release = response.body<GitHubRelease>()
            
            val latestVersion = release.tag_name.removePrefix("v")
            
            if (shouldUpdate(latestVersion)) {
                val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    val updateInfo = UpdateInfo(
                        version = latestVersion,
                        apkUrl = apkAsset.browser_download_url,
                        publishedAt = release.published_at
                    )
                    _updateAvailable.value = updateInfo
                    return updateInfo
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun shouldUpdate(latestVersion: String): Boolean {
        val current = CURRENT_VERSION.split(".").map { it.toIntOrNull() ?: 0 }
        val latest = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
        
        for (i in 0 until maxOf(current.size, latest.size)) {
            val c = current.getOrNull(i) ?: 0
            val l = latest.getOrNull(i) ?: 0
            
            if (l > c) return true
            if (l < c) return false
        }
        
        return false
    }

    fun dismissUpdate() {
        _updateAvailable.value = null
    }
}
