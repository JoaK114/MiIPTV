package com.app.mitvplayer.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Checks GitHub Releases for app updates and handles
 * download + installation of new APKs.
 */
class UpdateChecker(private val context: Context) {

    companion object {
        // TODO: Replace with actual repo after pushing to GitHub
        private const val GITHUB_OWNER = "OWNER"
        private const val GITHUB_REPO = "MiTVPlayer"
        private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Serializable
    data class GitHubRelease(
        @SerialName("tag_name") val tagName: String,
        val name: String? = null,
        val body: String? = null,
        val assets: List<GitHubAsset>? = null,
        @SerialName("published_at") val publishedAt: String? = null
    )

    @Serializable
    data class GitHubAsset(
        val name: String,
        @SerialName("browser_download_url") val downloadUrl: String,
        val size: Long = 0
    )

    data class UpdateInfo(
        val version: String,
        val changelog: String?,
        val downloadUrl: String,
        val fileSize: Long
    )

    /**
     * Check for updates. Returns UpdateInfo if a new version is available, null otherwise.
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "MiTVPlayer/$currentVersion")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val release = json.decodeFromString<GitHubRelease>(body)

            // Compare versions (strip 'v' prefix)
            val latestVersion = release.tagName.removePrefix("v")
            if (isNewerVersion(latestVersion, currentVersion)) {
                // Find APK asset
                val apkAsset = release.assets?.firstOrNull {
                    it.name.endsWith(".apk", ignoreCase = true)
                } ?: return@withContext null

                UpdateInfo(
                    version = latestVersion,
                    changelog = release.body,
                    downloadUrl = apkAsset.downloadUrl,
                    fileSize = apkAsset.size
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Download APK to cache and return the file.
     * @param onProgress callback with progress 0.0-1.0
     */
    suspend fun downloadApk(
        url: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MiTVPlayer/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val totalBytes = body.contentLength()

            val apkFile = File(context.cacheDir, "update.apk")
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (totalBytes > 0) {
                    onProgress(totalRead.toFloat() / totalBytes.toFloat())
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            apkFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Launch APK installation intent.
     */
    fun installApk(apkFile: File) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    /**
     * Compare version strings (e.g., "1.2.0" > "1.1.0")
     */
    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, localParts.size)

        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
