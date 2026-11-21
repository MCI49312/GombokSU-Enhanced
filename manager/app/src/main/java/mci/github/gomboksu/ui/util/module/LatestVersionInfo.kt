package mci.github.gomboksu.ui.util.module

data class LatestVersionInfo(
    val versionCode: Int = 0,
    val downloadUrl: String = "",
    val changelog: String = "",
    val versionTag: String = "v0.0.0",
    val versionName: String = "",
    val fileName: String = "",
    val isSpoofed: Boolean = false,
    val fileSize: Long = 0L
)
