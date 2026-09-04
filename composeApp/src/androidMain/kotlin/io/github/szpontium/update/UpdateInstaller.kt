package io.github.szpontium.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UpdateInstaller(private val context: Context, private val httpClient: HttpClient) {

    suspend fun downloadAndInstall(updateInfo: UpdateInfo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Pobierz APK
            val apkBytes = httpClient.get(updateInfo.apkUrl).readBytes()
            
            // Zapisz do cache
            val apkFile = File(context.cacheDir, "dziennik_${updateInfo.version}.apk")
            apkFile.writeBytes(apkBytes)
            
            // Zainstaluj
            installApk(apkFile)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun installApk(file: File) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(intent)
    }
}
