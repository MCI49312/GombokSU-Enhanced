package mci.github.gomboksu.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mci.github.gomboksu.BuildConfig
import mci.github.gomboksu.R
import mci.github.gomboksu.ui.LocalNavBarEnabled
import mci.github.gomboksu.ui.LocalScrollState
import mci.github.gomboksu.ui.MainActivity
import mci.github.gomboksu.ui.component.SwitchItem
import mci.github.gomboksu.ui.rememberScrollConnection
import mci.github.gomboksu.ui.util.*
import mci.github.gomboksu.ui.util.module.LatestVersionInfo
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun UpdaterScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? MainActivity }
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val bottomBarScrollState = LocalScrollState.current
    val bottomBarScrollConnection = if (bottomBarScrollState != null) {
        rememberScrollConnection(
            isScrollingDown = bottomBarScrollState.isScrollingDown,
            scrollOffset = bottomBarScrollState.scrollOffset,
            previousScrollOffset = bottomBarScrollState.previousScrollOffset,
            threshold = 30f
        )
    } else null

    val navBarEnabled = LocalNavBarEnabled.current?.value ?: true
    val navBarAutoHide = activity?.navBarAutoHideState?.value ?: prefs.getBoolean("navbar_auto_hide", true)
    val navBarStyle = activity?.navBarStyleState?.value ?: prefs.getInt("navbar_style", 0)
    val navBarShowLabels = activity?.navBarShowLabelsState?.value ?: prefs.getBoolean("navbar_show_labels", false)
    val isNavBarVisible = navBarEnabled && (!(bottomBarScrollState?.isScrollingDown?.value ?: false) || !navBarAutoHide)

    val systemNavBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val appNavBarClearance = when {
        !isNavBarVisible -> 8.dp
        navBarStyle == 1 -> 84.dp // Classic Docked NavigationBar
        navBarStyle == 2 -> 80.dp // Compact Pill (60dp + 20dp vertical padding)
        else -> if (navBarShowLabels) 104.dp else 100.dp // Floating Pill
    }

    val isSpoofed = remember { isCurrentAppSpoofed() }
    val buildTypeLabel = stringResource(if (isSpoofed) R.string.updater_build_spoofed else R.string.updater_build_normal)
    val (currentVersionName, currentVersionCode) = remember(context) { getManagerVersion(context) }

    var autoUpdateEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean("auto_update_app", true))
    }

    var isChecking by remember { mutableStateOf(true) }
    var latestInfo by remember { mutableStateOf(LatestVersionInfo(isSpoofed = isSpoofed)) }
    var checkError by remember { mutableStateOf<String?>(null) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val updateAvailable = remember(currentVersionName, currentVersionCode, latestInfo) {
        isUpdateAvailable(currentVersionName, currentVersionCode, latestInfo)
    }
    val hasValidAsset = latestInfo.downloadUrl.isNotBlank() && latestInfo.isSpoofed == isSpoofed

    fun refreshRelease() {
        if (isDownloading) return
        scope.launch {
            isChecking = true
            checkError = null
            statusMessage = null
            val info = withContext(Dispatchers.IO) {
                checkNewVersion(preferSpoofed = isSpoofed)
            }
            latestInfo = info
            if (info.downloadUrl.isBlank()) {
                checkError = context.getString(R.string.updater_no_matching_apk, buildTypeLabel)
            }
            isChecking = false
        }
    }

    LaunchedEffect(Unit) {
        refreshRelease()
    }

    fun startInstallFlow(actionLabel: String) {
        if (!hasValidAsset || isDownloading) return
        scope.launch {
            isDownloading = true
            downloadProgress = 0f
            statusMessage = context.getString(R.string.updater_downloading_start, actionLabel, latestInfo.fileName)

            val downloadResult = downloadUpdateApk(context, latestInfo) { progress, downloaded, total ->
                scope.launch(Dispatchers.Main) {
                    downloadProgress = progress
                    val dlMb = downloaded / (1024f * 1024f)
                    val totalMb = total / (1024f * 1024f)
                    statusMessage = if (total > 0L) {
                        context.getString(
                            R.string.updater_downloading_progress,
                            latestInfo.fileName,
                            dlMb,
                            totalMb
                        )
                    } else {
                        context.getString(R.string.updater_downloading_size, latestInfo.fileName, dlMb)
                    }
                }
            }

            downloadResult.onSuccess { apkFile ->
                downloadProgress = 1f
                statusMessage = context.getString(R.string.updater_installing, apkFile.name)
                val installResult = installDownloadedApk(context, apkFile, preferRootInstall = true)
                installResult.onSuccess { msg ->
                    statusMessage = msg
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    val errMsg = context.getString(
                        R.string.updater_install_failed,
                        err.message ?: context.getString(R.string.unknown_error)
                    )
                    statusMessage = errMsg
                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                }
            }.onFailure { err ->
                val errMsg = context.getString(
                    R.string.updater_download_failed,
                    err.message ?: context.getString(R.string.unknown_error)
                )
                statusMessage = errMsg
                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
            }

            isDownloading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.updater_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { navigator.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshRelease() },
                        enabled = !isChecking && !isDownloading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.updater_check_action)
                        )
                    }
                },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .let { modifier ->
                        if (bottomBarScrollConnection != null) {
                            modifier
                                .nestedScroll(bottomBarScrollConnection)
                                .nestedScroll(scrollBehavior.nestedScrollConnection)
                        } else {
                            modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                        }
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .padding(bottom = systemNavBarBottom + appNavBarClearance + 72.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Banner Card
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = when {
                            updateAvailable -> MaterialTheme.colorScheme.primaryContainer
                            checkError != null -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    updateAvailable -> Icons.Filled.SystemUpdate
                                    checkError != null -> Icons.Filled.ErrorOutline
                                    else -> Icons.Filled.CheckCircle
                                },
                                contentDescription = null,
                                modifier = Modifier.size(30.dp),
                                tint = when {
                                    updateAvailable -> MaterialTheme.colorScheme.onPrimaryContainer
                                    checkError != null -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isChecking -> stringResource(R.string.updater_checking)
                                    updateAvailable -> stringResource(R.string.updater_new_available, latestInfo.versionTag)
                                    checkError != null -> stringResource(R.string.updater_check_failed)
                                    else -> stringResource(R.string.updater_up_to_date)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = when {
                                    isChecking -> stringResource(R.string.updater_fetching, buildTypeLabel)
                                    updateAvailable -> stringResource(R.string.updater_ready_to_install, buildTypeLabel, latestInfo.fileName)
                                    checkError != null -> checkError!!
                                    else -> stringResource(R.string.updater_running_latest, buildTypeLabel, currentVersionName)
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Progress Card while downloading / installing
                AnimatedVisibility(visible = isDownloading || statusMessage != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (isDownloading) {
                                if (downloadProgress >= 0f) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }

                // Version & Build Variant Details Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.updater_version_details),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        UpdaterDetailRow(
                            icon = Icons.Filled.Info,
                            label = stringResource(R.string.updater_installed_manager),
                            value = "$currentVersionName ($currentVersionCode)"
                        )

                        UpdaterDetailRow(
                            icon = if (isSpoofed) Icons.Filled.VisibilityOff else Icons.Filled.Verified,
                            label = stringResource(R.string.updater_detected_build_type),
                            value = "$buildTypeLabel (${context.packageName})"
                        )

                        UpdaterDetailRow(
                            icon = Icons.Filled.NewReleases,
                            label = stringResource(R.string.updater_latest_github_release),
                            value = if (latestInfo.versionCode > 0) {
                                "${latestInfo.versionTag} (${latestInfo.versionCode})"
                            } else if (isChecking) {
                                stringResource(R.string.updater_checking_short)
                            } else {
                                stringResource(R.string.unavailable)
                            }
                        )

                        if (latestInfo.fileName.isNotBlank()) {
                            UpdaterDetailRow(
                                icon = Icons.Filled.Android,
                                label = stringResource(R.string.updater_matched_release_file),
                                value = latestInfo.fileName
                            )
                        }
                    }
                }

                // Auto-Update Settings Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        SwitchItem(
                            icon = Icons.Filled.AutoMode,
                            title = stringResource(R.string.updater_auto_update),
                            summary = stringResource(R.string.updater_auto_update_summary, buildTypeLabel),
                            checked = autoUpdateEnabled,
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) { checked ->
                            autoUpdateEnabled = checked
                            prefs.edit { putBoolean("auto_update_app", checked) }
                        }
                    }
                }

                // Formatted Release Notes Card (no raw markdown symbols)
                if (latestInfo.changelog.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.updater_release_notes, latestInfo.versionTag),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FormattedReleaseNotes(markdown = latestInfo.changelog.trim())
                        }
                    }
                }
            }

            // Sleek floating bottom action bar — adapts cleanly to Floating Pill, Classic Docked, Compact Pill, or Hidden Nav Bar
            val reinstallingLabel = stringResource(R.string.updater_reinstalling)
            val updatingLabel = stringResource(R.string.updater_updating)
            Surface(
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = systemNavBarBottom + appNavBarClearance)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { startInstallFlow(reinstallingLabel) },
                        enabled = hasValidAsset && !isChecking && !isDownloading,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.updater_reinstall_app),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { startInstallFlow(updatingLabel) },
                        enabled = updateAvailable && hasValidAsset && !isChecking && !isDownloading,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.updater_update_to_new),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormattedReleaseNotes(markdown: String) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val codeBgColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val lines = remember(markdown) {
        markdown.replace("\r\n", "\n").lines()
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed.isEmpty() || trimmed == "---" || trimmed == "***" -> {
                    Spacer(Modifier.height(2.dp))
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("### ").trim(), onSurfaceColor, primaryColor, codeBgColor),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("## ").trim(), onSurfaceColor, primaryColor, codeBgColor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("# ").trim(), onSurfaceColor, primaryColor, codeBgColor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = onSurfaceColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val itemContent = trimmed.drop(2).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = parseInlineMarkdown(itemContent, onSurfaceColor, primaryColor, codeBgColor),
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(trimmed, onSurfaceColor, primaryColor, codeBgColor),
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor
                    )
                }
            }
        }
    }
}

private fun parseInlineMarkdown(
    text: String,
    boldColor: Color,
    linkColor: Color,
    codeBgColor: Color
): AnnotatedString {
    val tokenRegex = Regex("""\*\*(.+?)\*\*|`([^`]+)`|\[([^\]]+)]\(([^)]+)\)|\*([^*]+)\*""")
    return buildAnnotatedString {
        var cursor = 0
        for (match in tokenRegex.findAll(text)) {
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }
            when {
                match.groups[1] != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                        append(match.groupValues[1])
                    }
                }
                match.groups[2] != null -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBgColor,
                            color = boldColor
                        )
                    ) {
                        append(" ${match.groupValues[2]} ")
                    }
                }
                match.groups[3] != null -> {
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(match.groupValues[3])
                    }
                }
                match.groups[5] != null -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(match.groupValues[5])
                    }
                }
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

@Composable
private fun UpdaterDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
