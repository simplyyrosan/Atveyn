package com.atveyn.app.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val latestVersion: String,
    val isUpdateAvailable: Boolean,
    val releaseNotes: String,
    val downloadUrl: String?,
    val htmlUrl: String,
    val publishedAt: String,
    val currentVersion: String = "1.6"
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class UpToDate(val currentVersion: String) : UpdateState
    data class Downloading(val progress: Float, val bytesRead: Long, val totalBytes: Long) : UpdateState
    data class ReadyToInstall(val file: File, val info: UpdateInfo) : UpdateState
    data class Error(val message: String) : UpdateState
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun checkForUpdate(
        owner: String = "simplyyrosan",
        repo: String = "Atveyn"
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Atveyn-Android-App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext Result.failure(Exception("No releases found on GitHub ($owner/$repo)."))
            }
            if (responseCode !in 200..299) {
                return@withContext Result.failure(Exception("GitHub API returned error code $responseCode."))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val json = JSONObject(responseBody)

            val rawTag = json.optString("tag_name", "")
            val cleanVersion = rawTag.removePrefix("v").removePrefix("V").trim()
            val currentVersion = getAppVersionName(context).trim()

            val isAvailable = isNewerVersion(cleanVersion, currentVersion)
            val releaseNotes = json.optString("body", "No release notes provided.")
            val htmlUrl = json.optString("html_url", "https://github.com/$owner/$repo/releases")
            val publishedAt = json.optString("published_at", "")

            var downloadUrl: String? = null
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = if (asset.has("browser_download_url")) asset.getString("browser_download_url") else null
                        break
                    }
                }
            }

            Result.success(
                UpdateInfo(
                    latestVersion = cleanVersion,
                    isUpdateAvailable = isAvailable,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    htmlUrl = htmlUrl,
                    publishedAt = publishedAt,
                    currentVersion = currentVersion
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadApk(
        downloadUrl: String,
        version: String,
        onProgress: (Float, Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Sanitize version to prevent path manipulation
            val safeVersion = version.replace(Regex("[^a-zA-Z0-9._-]"), "")
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val targetFile = File(updatesDir, "Atveyn-$safeVersion.apk")

            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirectCount = 0

            // Follow HTTP redirects (GitHub releases redirect to AWS S3)
            while (true) {
                val url = URL(currentUrl)
                // Require HTTPS for security
                if (!url.protocol.equals("https", ignoreCase = true)) {
                    return@withContext Result.failure(SecurityException("Insecure download URL (HTTPS required)"))
                }

                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "Atveyn-Android-App")
                    instanceFollowRedirects = false
                }

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == HttpURLConnection.HTTP_SEE_OTHER ||
                    code == 307
                ) {
                    val location = connection.getHeaderField("Location")
                        ?: return@withContext Result.failure(Exception("Redirect without location header"))
                    connection.disconnect()
                    currentUrl = location
                    redirectCount++
                    if (redirectCount > 5) {
                        return@withContext Result.failure(Exception("Too many redirects"))
                    }
                    continue
                }
                break
            }

            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return@withContext Result.failure(Exception("Download failed with HTTP ${connection.responseCode}"))
            }

            val totalBytes = connection.contentLengthLong
            var bytesReadTotal = 0L

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesReadTotal += read
                        val progress = if (totalBytes > 0) {
                            bytesReadTotal.toFloat() / totalBytes.toFloat()
                        } else {
                            -1f
                        }
                        onProgress(progress, bytesReadTotal, totalBytes)
                    }
                }
            }
            connection.disconnect()

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return Result.failure(Exception("Update file is missing or corrupted. Please download again."))
            }

            // Check "Install unknown apps" permission on Android 8+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return Result.failure(
                        Exception("Please allow Atveyn to install apps, then tap 'Install Update Now' again.")
                    )
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            // Explicitly grant read URI permission to common installer packages
            val installerPackages = listOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller"
            )
            for (pkg in installerPackages) {
                try {
                    context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        return AppVersionComparator.isNewerVersion(remoteVersion, currentVersion)
    }

    companion object {
        fun getAppVersionName(context: Context): String {
            return try {
                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                pInfo.versionName ?: "1.6"
            } catch (e: Exception) {
                "1.6"
            }
        }
    }
}

object AppVersionComparator {
    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        if (remoteVersion.isBlank()) return false
        val rClean = remoteVersion.removePrefix("v").removePrefix("V").trim()
        val cClean = currentVersion.removePrefix("v").removePrefix("V").trim()
        if (rClean.equals(cClean, ignoreCase = true)) return false

        try {
            val rTokens = rClean.split('.')
            val cTokens = cClean.split('.')

            val maxLen = maxOf(rTokens.size, cTokens.size)
            for (i in 0 until maxLen) {
                val rStr = rTokens.getOrNull(i) ?: "0"
                val cStr = cTokens.getOrNull(i) ?: "0"

                val rInt = rStr.toIntOrNull()
                val cInt = cStr.toIntOrNull()

                if (rInt != null && cInt != null) {
                    // Handle 2-digit patch compression without second dot (e.g. "6" vs "52")
                    if (i == 1 && rStr.length != cStr.length && rStr.length <= 2 && cStr.length <= 2) {
                        val rNormalized = rStr.padEnd(2, '0').toInt()
                        val cNormalized = cStr.padEnd(2, '0').toInt()
                        if (rNormalized > cNormalized) return true
                        if (rNormalized < cNormalized) return false
                    } else {
                        if (rInt > cInt) return true
                        if (rInt < cInt) return false
                    }
                } else {
                    val comp = rStr.compareTo(cStr)
                    if (comp > 0) return true
                    if (comp < 0) return false
                }
            }
        } catch (e: Exception) {
            return rClean != cClean
        }
        return false
    }
}
