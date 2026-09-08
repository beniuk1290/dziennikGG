package pl.szczodrzynski.edziennik.data.api.edziennik.eduvulcan

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.eduvulcan.login.EduVulcanLoginPrometheus
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe.VulcanHebeMain
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.login.VulcanLoginHebe
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ext.getString
import timber.log.Timber

class EduVulcanFirstLogin(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        const val TAG = "EduVulcanFirstLogin"
    }

    private val profileList = mutableListOf<Profile>()
    private val loginStoreId = data.loginStore.id
    private var firstProfileId = loginStoreId

    init {
        val email = data.loginStore.getLoginData("email", "")
        val password = data.loginStore.getLoginData("password", "")

        if (email.isEmpty() || password.isEmpty()) {
            data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            return
        }

        Timber.d("$TAG: Starting eduVULCAN first login with email: $email")

        EduVulcanLoginPrometheus(
            data = data,
            onSuccess = { tokens, tenantTokens ->
                Timber.d("$TAG: Got ${tokens.size} tokens, ${tenantTokens.size} tenants")
                processTokens(tokens, tenantTokens)
            },
            onError = { errorCode, errorMessage ->
                Timber.e("$TAG: Login failed: $errorCode - $errorMessage")
                data.error(ApiError(TAG, errorCode))
            }
        ).login(email, password)
    }

    private fun processTokens(tokens: List<String>, tenantTokens: Map<String, String>) {
        if (tenantTokens.isEmpty()) {
            data.error(ApiError(TAG, ERROR_LOGIN_VULCAN_INVALID_TOKEN))
            return
        }

        val firstTenant = tenantTokens.keys.first()
        val firstToken = tenantTokens[firstTenant]!!

        Timber.d("$TAG: Using tenant: $firstTenant")

        data.apiToken = data.apiToken.toMutableMap().also {
            it[firstTenant] = firstToken
        }

        registerDeviceHebe(firstTenant, tokens)
    }

    private fun registerDeviceHebe(tenant: String, allTokens: List<String>) {
        Timber.d("$TAG: Registering device with Hebe for tenant: $tenant")

        data.symbol = tenant

        VulcanLoginHebe(data) {
            Timber.d("$TAG: Device registered, fetching students")
            VulcanHebeMain(data).getStudents(
                profile = null,
                profileList,
                loginStoreId,
                firstProfileId,
                onEmpty = {
                    Timber.w("$TAG: No students found")
                    EventBus.getDefault()
                        .postSticky(FirstLoginFinishedEvent(listOf(), data.loginStore))
                    onSuccess()
                },
                onSuccess = {
                    Timber.d("$TAG: Got ${profileList.size} students")
                    EventBus.getDefault().postSticky(FirstLoginFinishedEvent(profileList, data.loginStore))
                    onSuccess()
                }
            )
        }
    }
}
