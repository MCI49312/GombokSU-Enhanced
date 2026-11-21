package mci.github.gomboksu.ui.util

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mci.github.gomboksu.BuildConfig
import mci.github.gomboksu.ksuApp
import mci.github.gomboksu.ui.util.module.LatestVersionInfo
import java.io.File
import java.io.FileOutputStream

/**
 * @author weishu
 * @date 2023/6/22.
 */
private const val UPDATER_TAG = "GombokUpdater"
private val NORMAL_PACKAGE_ID = listOf("mci", "github", "gomboksu").joinToString(".")

fun isCurrentAppSpoofed(): Boolean {
    return BuildConfig.VERSION_NAME.contains("spoofed", ignoreCase = true) ||
        BuildConfig.APPLICATION_ID != NORMAL_PACKAGE_ID
}

fun parseSemVer(raw: String): List<Int> {
    val clean = raw.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore("-")
        .substringBefore("_")
        .substringBefore(" ")
    val match = Regex("""^(\d+(?:\.\d+)*)""").find(clean) ?: return emptyList()
    return match.groupValues[1].split(".").mapNotNull { it.toIntOrNull() }
}

fun compareSemVer(a: List<Int>, b: List<Int>): Int {
    val maxLen = maxOf(a.size, b.size)
    for (i in 0 until maxLen) {
        val va = a.getOrElse(i) { 0 }
        val vb = b.getOrElse(i) { 0 }
        if (va != vb) return va.compareTo(vb)
    }
    return 0
}

fun isUpdateAvailable(
    currentVersionName: String,
    currentVersionCode: Long,
    remote: LatestVersionInfo
): Boolean {
    if (remote.downloadUrl.isBlank() || remote.fileName.isBlank()) return false
    // Strictly prevent mixing Normal and Spoofed builds
    if (remote.isSpoofed != isCurrentAppSpoofed()) return false

    val currentSem = parseSemVer(currentVersionName)
    val remoteSem = parseSemVer(remote.versionName.ifBlank { remote.versionTag })

    if (currentSem.isNotEmpty() && remoteSem.isNotEmpty()) {
        val cmp = compareSemVer(remoteSem, currentSem)
        if (cmp > 0) return true
        if (cmp < 0) return false
        // Same semantic version (e.g. v3.4.2 vs v3.4.2): only update if remote versionCode is strictly higher
        return remote.versionCode > currentVersionCode
    }
    return remote.versionCode > currentVersionCode
}

@SuppressLint("Range")
fun download(
    context: Context,
    url: String,
    fileName: String,
    description: String,
    onDownloaded: (Uri) -> Unit = {},
    onDownloading: () -> Unit = {}
) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val query = DownloadManager.Query()
    query.setFilterByStatus(DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_PENDING)
    downloadManager.query(query).use { cursor ->
        while (cursor.moveToNext()) {
            val uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI))
            val localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val columnTitle = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
            if (url == uri || fileName == columnTitle) {
                if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) {
                    onDownloading()
                    return
                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    onDownloaded(localUri.toUri())
                    return
                }
            }
        }
    }

    val request = DownloadManager.Request(url.toUri())
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            fileName
        )
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setMimeType("application/zip")
        .setTitle(fileName)
        .setDescription(description)

    downloadManager.enqueue(request)
}

fun checkNewVersion(preferSpoofed: Boolean? = null): LatestVersionInfo {
    val url = "https://api.github.com/repos/MCI49312/GombokSU-Enhanced/releases/latest"
    val targetSpoofed = preferSpoofed ?: isCurrentAppSpoofed()
    val defaultValue = LatestVersionInfo(isSpoofed = targetSpoofed)

    runCatching {
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .build()
        ksuApp.okhttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return defaultValue
            }
            val body = response.body?.string() ?: return defaultValue
            val json = org.json.JSONObject(body)
            val changelog = json.optString("body")
            val versionTag = json.optString("tag_name", "")

            val assets = json.optJSONArray("assets") ?: return defaultValue
            val fileRegex = Regex("""v([0-9]+(?:\.[0-9]+)*)(?:-spoofed)?_(\d+)-""", RegexOption.IGNORE_CASE)

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (!name.endsWith(".apk", ignoreCase = true)) continue

                val isAssetSpoofed = name.contains("spoofed", ignoreCase = true)
                // Strict variant matching: never confuse Normal with Spoofed or vice versa
                if (isAssetSpoofed != targetSpoofed) continue

                val match = fileRegex.find(name)
                val parsedVersionName = match?.groupValues?.getOrNull(1)?.let { "v$it" }
                    ?: versionTag
                val parsedVersionCode = match?.groupValues?.getOrNull(2)?.toIntOrNull()
                    ?: Regex("""_(\d+)-""").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: 0
                val downloadUrl = asset.optString("browser_download_url", "")
                val fileSize = asset.optLong("size", 0L)

                if (downloadUrl.isNotBlank()) {
                    return LatestVersionInfo(
                        versionCode = parsedVersionCode,
                        downloadUrl = downloadUrl,
                        changelog = changelog,
                        versionTag = versionTag.ifBlank { parsedVersionName },
                        versionName = parsedVersionName,
                        fileName = name,
                        isSpoofed = isAssetSpoofed,
                        fileSize = fileSize
                    )
                }
            }
        }
    }.onFailure { e ->
        Log.w(UPDATER_TAG, "Failed to check latest release", e)
    }
    return defaultValue
}

suspend fun downloadUpdateApk(
    context: Context,
    versionInfo: LatestVersionInfo,
    onProgress: (Float, Long, Long) -> Unit
): Result<File> = withContext(Dispatchers.IO) {
    runCatching {
        require(versionInfo.downloadUrl.isNotBlank()) { "Download URL is empty" }
        require(versionInfo.isSpoofed == isCurrentAppSpoofed()) {
            "Build type mismatch: refusing to install ${if (versionInfo.isSpoofed) "Spoofed" else "Normal"} over ${if (isCurrentAppSpoofed()) "Spoofed" else "Normal"}"
        }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val safeName = versionInfo.fileName.ifBlank { "GombokSU_Enhanced_update.apk" }
        val targetFile = File(updatesDir, safeName)
        val tmpFile = File(updatesDir, "$safeName.part")
        if (tmpFile.exists()) tmpFile.delete()

        val request = okhttp3.Request.Builder().url(versionInfo.downloadUrl).build()
        ksuApp.okhttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}: failed to download APK")
            }
            val body = response.body ?: error("Empty response body")
            val totalBytes = body.contentLength().takeIf { it > 0L } ?: versionInfo.fileSize
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val progress = if (totalBytes > 0L) {
                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            -1f
                        }
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                    output.flush()
                }
            }
        }

        require(tmpFile.exists() && tmpFile.length() > 1024L) { "Downloaded APK is invalid or empty" }
        if (targetFile.exists()) targetFile.delete()
        check(tmpFile.renameTo(targetFile)) { "Failed to finalize APK file" }
        targetFile
    }
}

suspend fun installDownloadedApk(
    context: Context,
    apkFile: File,
    preferRootInstall: Boolean = true
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        require(apkFile.exists() && apkFile.length() > 1024L) { "APK file not found" }

        if (preferRootInstall && rootAvailable()) {
            val tmpPath = "/data/local/tmp/${apkFile.name.replace("'", "")}"
            val pkg = BuildConfig.APPLICATION_ID
            val isSamePackage = !isCurrentAppSpoofed()
            if (isSamePackage) {
                // Detached root install so Android replacing the running package still relaunches cleanly
                val detachedCmd = """
                    cp '${apkFile.absolutePath}' '$tmpPath' && \
                    chmod 644 '$tmpPath' && \
                    nohup sh -c "pm install -r -d '$tmpPath' && rm -f '$tmpPath' && sleep 1 && monkey -p '$pkg' -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1 &
                """.trimIndent().replace("\n", " ")
                val launched = withNewRootShell(globalMnt = true) {
                    newJob().add(detachedCmd).exec().isSuccess
                }
                if (launched) {
                    return@runCatching "Installing via root… the app will relaunch automatically."
                }
            } else {
                // Spoofed build has a new random package ID, so pm install won't kill this process
                val cmd = "cp '${apkFile.absolutePath}' '$tmpPath' && chmod 644 '$tmpPath' && pm install -r -d '$tmpPath'; rm -f '$tmpPath'"
                val res = withNewRootShell(globalMnt = true) {
                    newJob().add(cmd).exec()
                }
                if (res.isSuccess) {
                    return@runCatching "Installed new Spoofed build via root."
                }
            }
        }

        // Standard Android PackageInstaller prompt via FileProvider
        withContext(Dispatchers.Main) {
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        "Opened system installer."
    }
}

@Composable
fun DownloadListener(context: Context, onDownloaded: (Uri) -> Unit) {
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("Range")
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, -1
                    )
                    val query = DownloadManager.Query().setFilterById(id)
                    val downloadManager =
                        context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(
                            cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        )
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val uri = cursor.getString(
                                cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            )
                            onDownloaded(uri.toUri())
                        }
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}
