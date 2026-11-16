package com.x2tora.torabox.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseApkAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

data class RemoteReleaseInfo(
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val selectedAsset: ReleaseApkAsset,
    val matchedAbiLabel: String
)

sealed interface UpdateCheckOutcome {
    data class UpdateAvailable(val release: RemoteReleaseInfo) : UpdateCheckOutcome
    data class UpToDate(val latestVersion: String) : UpdateCheckOutcome
    data class Failed(val message: String) : UpdateCheckOutcome
}

enum class InstallLaunchOutcome {
    LAUNCHED_INSTALLER,
    NEEDS_UNKNOWN_SOURCE_PERMISSION,
    FAILED
}

object AppUpdater {
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/x2tora/tora-box/releases/latest"

    fun parseVersionSegments(version: String): List<Int> {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
        if (cleaned.isBlank()) return emptyList()
        return cleaned.split('.').map { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
    }

    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        val remote = parseVersionSegments(remoteVersion)
        val current = parseVersionSegments(currentVersion)
        if (remote.isEmpty() || current.isEmpty()) return false
        val maxLen = maxOf(remote.size, current.size)
        for (i in 0 until maxLen) {
            val r = remote.getOrElse(i) { 0 }
            val c = current.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun selectBestApkAsset(
        assets: List<ReleaseApkAsset>,
        supportedAbis: List<String>
    ): Pair<ReleaseApkAsset, String>? {
        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

        for (abi in supportedAbis) {
            val matched = apkAssets.firstOrNull {
                it.name.contains("-$abi.apk", ignoreCase = true)
            }
            if (matched != null) return matched to abi
        }

        val universal = apkAssets.firstOrNull {
            it.name.contains("universal", ignoreCase = true)
        }
        if (universal != null) return universal to "universal"

        return apkAssets.first() to "apk"
    }

    suspend fun checkLatestRelease(
        currentVersionName: String,
        supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList()
    ): UpdateCheckOutcome = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(LATEST_RELEASE_API)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "ToraBox-Android/$currentVersionName")
            }

            val status = conn.responseCode
            if (status == 404) {
                return@withContext UpdateCheckOutcome.UpToDate(currentVersionName)
            }
            if (status !in 200..299) {
                return@withContext UpdateCheckOutcome.Failed("서버 응답 오류 (HTTP $status)")
            }

            val bodyText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(bodyText)
            if (json.optBoolean("draft", false)) {
                return@withContext UpdateCheckOutcome.UpToDate(currentVersionName)
            }

            val tagName = json.optString("tag_name", "").trim()
            val remoteClean = tagName.removePrefix("v").removePrefix("V")
            if (!isNewerVersion(remoteClean, currentVersionName)) {
                return@withContext UpdateCheckOutcome.UpToDate(remoteClean.ifBlank { currentVersionName })
            }

            val assetsJson = json.optJSONArray("assets")
            val parsedAssets = buildList {
                if (assetsJson != null) {
                    for (i in 0 until assetsJson.length()) {
                        val item = assetsJson.optJSONObject(i) ?: continue
                        val name = item.optString("name", "")
                        val downloadUrl = item.optString("browser_download_url", "")
                        val size = item.optLong("size", 0L)
                        if (name.isNotBlank() && downloadUrl.isNotBlank()) {
                            add(ReleaseApkAsset(name = name, downloadUrl = downloadUrl, sizeBytes = size))
                        }
                    }
                }
            }

            val (selectedAsset, abiLabel) = selectBestApkAsset(parsedAssets, supportedAbis)
                ?: return@withContext UpdateCheckOutcome.Failed("설치 가능한 APK 파일을 찾지 못했습니다.")

            val title = json.optString("name", tagName).ifBlank { tagName }
            val notes = json.optString("body", "").trim()
            val htmlUrl = json.optString("html_url", "")

            UpdateCheckOutcome.UpdateAvailable(
                RemoteReleaseInfo(
                    versionName = remoteClean.ifBlank { tagName },
                    releaseTitle = title,
                    releaseNotes = notes,
                    htmlUrl = htmlUrl,
                    selectedAsset = selectedAsset,
                    matchedAbiLabel = abiLabel
                )
            )
        } catch (e: Exception) {
            UpdateCheckOutcome.Failed(e.localizedMessage ?: "네트워크 연결을 확인해 주세요.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun downloadApk(
        context: Context,
        asset: ReleaseApkAsset,
        onProgress: (Float, Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            updatesDir.listFiles()?.forEach { old -> old.delete() }

            val targetFile = File(updatesDir, "torabox-update.apk")
            val tempFile = File(updatesDir, "torabox-update.apk.part")

            var activeUrl = asset.downloadUrl
            var redirects = 0
            while (true) {
                conn?.disconnect()
                conn = (URL(activeUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 12000
                    readTimeout = 20000
                    setRequestProperty("User-Agent", "ToraBox-Android")
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                        ?: error("다운로드 리다이렉트 경로가 비어 있습니다.")
                    if (++redirects > 6) error("리다이렉트 횟수를 초과했습니다.")
                    activeUrl = URL(URL(activeUrl), loc).toString()
                    continue
                }
                if (code !in 200..299) {
                    error("APK 다운로드 실패 (HTTP $code)")
                }
                break
            }

            val totalBytes = conn.contentLengthLong.takeIf { it > 0L } ?: asset.sizeBytes
            var downloaded = 0L
            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var read: Int
                    var lastEmitMs = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        val nowMs = System.currentTimeMillis()
                        if (nowMs - lastEmitMs >= 80L) {
                            lastEmitMs = nowMs
                            val progress = if (totalBytes > 0L) {
                                (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            onProgress(progress, downloaded, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            if (targetFile.exists()) targetFile.delete()
            check(tempFile.renameTo(targetFile)) { "다운로드한 APK 파일을 준비할 수 없습니다." }
            onProgress(1f, downloaded, maxOf(totalBytes, downloaded))
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun openInstallPermissionSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return runCatching {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun launchApkInstaller(context: Context, apkFile: File): InstallLaunchOutcome {
        if (!apkFile.exists()) return InstallLaunchOutcome.FAILED
        if (!canInstallPackages(context)) {
            openInstallPermissionSettings(context)
            return InstallLaunchOutcome.NEEDS_UNKNOWN_SOURCE_PERMISSION
        }
        return runCatching {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            InstallLaunchOutcome.LAUNCHED_INSTALLER
        }.getOrDefault(InstallLaunchOutcome.FAILED)
    }

    fun cleanupCache(context: Context) {
        runCatching {
            File(context.cacheDir, "updates").listFiles()?.forEach { it.delete() }
        }
    }
}
