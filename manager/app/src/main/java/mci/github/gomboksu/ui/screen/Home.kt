package mci.github.gomboksu.ui.screen

import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.PowerManager
import android.system.Os
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.dergoogler.mmrl.ui.component.text.TextRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ContributorsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.generated.destinations.UpdaterScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import mci.github.gomboksu.*
import mci.github.gomboksu.R
import mci.github.gomboksu.ui.component.rememberConfirmDialog
import mci.github.gomboksu.ui.theme.ORANGE
import mci.github.gomboksu.ui.util.*
import mci.github.gomboksu.ui.webui.WebUIActivity
import mci.github.gomboksu.ui.util.restartActivity
import mci.github.gomboksu.ui.util.module.LatestVersionInfo
import mci.github.gomboksu.ui.viewmodel.ModuleViewModel
import mci.github.gomboksu.ui.LocalNavBarEnabled
import mci.github.gomboksu.ui.LocalScrollState 
import mci.github.gomboksu.ui.screen.BottomBarDestination
import mci.github.gomboksu.ui.trackScroll 
import mci.github.gomboksu.ui.rememberScrollConnection
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val kernelVersion = getKernelVersion()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val isManager = Natives.isManager
    val fullFeatured = Natives.isFullFeatured()
    val ksuVersion = if (isManager) Natives.version else null
    val ksuVersionTag = if (isManager) Natives.getVersionTag() else null
    val kernelUAPIVersion = if (isManager) Natives.kernelUAPIVersion else null
    val managerUAPIVersion = Natives.managerUAPIVersion

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? mci.github.gomboksu.ui.MainActivity }
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)
    val showAboutDevice = prefs.getBoolean("home_show_about_device", true)
    val autoExpandInfo = prefs.getBoolean("home_auto_expand_info", false)
    val showContributors = prefs.getBoolean("home_show_contributors", true)
    val showIssueReport = prefs.getBoolean("home_show_issue_report", true)
    val customTitle = prefs.getString("custom_header_title", "GombokSU Enhanced")
        ?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name)
    val customSubtitle = prefs.getString("custom_header_subtitle", "") ?: ""
    val appIconKey = activity?.appIconKeyState?.value ?: (prefs.getString("app_icon_key", "gomboksu") ?: "gomboksu")
    val topBarLogoRes = remember(appIconKey) { AppIconPreset.fromKey(appIconKey).vectorDrawable }
    
    // Get scroll state for bottom bar tracking
    val bottomBarScrollState = LocalScrollState.current

    val scrollState = LocalScrollState.current
    val navBarEnabled = LocalNavBarEnabled.current
    val isNavBarHidden = (scrollState?.isScrollingDown?.value ?: false) || (navBarEnabled?.value == false)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (isNavBarHidden) 0.dp else 112.dp
    
    // Create scroll connection for bottom bar
    val bottomBarScrollConnection = if (bottomBarScrollState != null) {
        rememberScrollConnection(
            isScrollingDown = bottomBarScrollState.isScrollingDown,
            scrollOffset = bottomBarScrollState.scrollOffset,
            previousScrollOffset = bottomBarScrollState.previousScrollOffset,
            threshold = 30f
        )
    } else null

    Scaffold(
        topBar = {
            TopBar(
                kernelVersion,
                ksuVersion,
                customTitle = customTitle,
                customSubtitle = customSubtitle,
                logoResId = topBarLogoRes,
                onInstallClick = {
                    navigator.navigate(InstallScreenDestination)
                },
                onSettingsClick = {
                    navigator.navigate(SettingScreenDestination) {
                        popUpTo(NavGraphs.root.startRoute) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                navBarEnabled = LocalNavBarEnabled.current?.value ?: true,
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                // Chain scroll connections - bottom bar tracking first, then topbar behavior
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
                .padding(top = 16.dp)
                .padding(bottom = navBarPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val lkmMode = ksuVersion?.let {
                Natives.isLkmMode
            }

            GombokAnimatedEntrance(index = 0) {
                StatusCard(
                    kernelVersion,
                    ksuVersion,
                    kernelUAPIVersion,
                    lkmMode,
                    ksuVersionTagParam = ksuVersionTag
                ) {
                    navigator.navigate(InstallScreenDestination)
                }
            }

            val homeDestination = BottomBarDestination.entries.firstOrNull()
            val startRoute = homeDestination?.direction?.route

            if (fullFeatured) {
                GombokAnimatedEntrance(index = 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SuperuserCard(
                                onClick = {
                                    navigator.navigate(SuperUserScreenDestination) {
                                        popUpTo(NavGraphs.root.startRoute) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            ModuleCard(
                                onClick = {
                                    navigator.navigate(ModuleScreenDestination) {
                                        popUpTo(NavGraphs.root.startRoute) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            val currentVersionCode = getManagerVersion(context).second
            val requiresNewKernel = isManager && kernelUAPIVersion != null && managerUAPIVersion > kernelUAPIVersion
            val requiresNewManager = isManager && kernelUAPIVersion != null && managerUAPIVersion < kernelUAPIVersion

            // Jailbreak mode runs on locked bootloaders, so flashing a boot image would brick the device.
            val canInstallKernelUpdate = lkmMode == true && !Natives.isLateLoadMode

            if (requiresNewKernel) {
                WarningCard(
                    stringResource(
                        id = if (canInstallKernelUpdate) R.string.require_kernel_version else R.string.require_kernel_version_gki,
                        kernelUAPIVersion!!,
                        managerUAPIVersion
                    ),
                    onClick = if (canInstallKernelUpdate) {
                        { navigator.navigate(InstallScreenDestination) }
                    } else null
                )
            }

            if (requiresNewManager) {
                WarningCard(
                    stringResource(
                        id = R.string.require_manager_version,
                        managerUAPIVersion,
                        kernelUAPIVersion!!
                    )
                )
            }

            val showLkmUpdate = isManager && lkmMode == true && Natives.isLkmBundled && ksuVersion?.toLong() != currentVersionCode && !requiresNewKernel && !requiresNewManager

            if (showLkmUpdate) {
                WarningCard(
                    message = stringResource(R.string.home_lkm_update_available),
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { navigator.navigate(InstallScreenDestination) }
                )
            }

            if (ksuVersion != null && !rootAvailable()) {
                WarningCard(
                    stringResource(id = R.string.grant_root_failed),
                    onClick = {
                        restartActivity(context)
                    }
                )
            }

            val checkUpdate =
                LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getBoolean("check_update", true)
            if (checkUpdate) {
                UpdateCard(
                    onOpenUpdater = {
                        navigator.navigate(UpdaterScreenDestination)
                    }
                )
            }

            if (showAboutDevice) {
                GombokAnimatedEntrance(index = 2) {
                    InfoCard(autoExpand = autoExpandInfo)
                }
            }
            if (showIssueReport) {
                GombokAnimatedEntrance(index = 3) {
                    IssueReportCard()
                }
            }
            GombokAnimatedEntrance(index = 4) {
                UpdaterLinkCard(
                    onClick = {
                        navigator.navigate(UpdaterScreenDestination)
                    }
                )
            }
            if (showContributors) {
                GombokAnimatedEntrance(index = 5) {
                    ContributorsCard(
                        onClick = {
                            navigator.navigate(ContributorsScreenDestination)
                        }
                    )
                }
            }
            Spacer(Modifier)
        }
    }
}

@Composable
private fun SuperuserCard(onClick: (() -> Unit)? = null) {
    val count = getSuperuserCount()
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .gombokPressBounce()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (count <= 1) {
                        stringResource(R.string.home_superuser_count_singular)
                    } else {
                        stringResource(R.string.home_superuser_count_plural)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(onClick: (() -> Unit)? = null) {
    val count = getModuleCount()
    val moduleViewModel: ModuleViewModel = viewModel()

    val moduleUpdateCount = remember(moduleViewModel.moduleList) {
        moduleViewModel.moduleList.count { module ->
            module.enabled && (moduleViewModel.checkUpdate(module).first.isNotEmpty())
        }
    }

    // State machine: 0 = nothing, 1 = show "+ Update!", 2 = show "+ X"
    var step by remember { mutableStateOf(0) }

    LaunchedEffect(moduleUpdateCount) {
        if (moduleUpdateCount > 0) {
            step = 1
            delay(1200) // show "+ Update!" for a moment
            step = 2
        } else {
            step = 0
        }
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .gombokPressBounce()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (count <= 1) {
                        stringResource(R.string.home_module_count_singular)
                    } else {
                        stringResource(R.string.home_module_count_plural)
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (moduleUpdateCount > 0) {
                        Spacer(Modifier.width(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Keep the "|" static
                            Text(
                                text = "|",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.width(4.dp))

                            // Animate only the right-side text
                            AnimatedContent(
                                targetState = step,
                                transitionSpec = {
                                    slideInHorizontally { -it } + fadeIn() togetherWith
                                            slideOutHorizontally { it } + fadeOut()
                                }
                            ) { target ->
                                when (target) {
                                    1 -> Text(
                                        text = stringResource(id = R.string.home_module_update_available),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    2 -> Text(
                                        text = buildAnnotatedString {
                                            append(moduleUpdateCount.toString())
                                            append("*")
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private var lastAutoUpdatedTag: String? = null

@Composable
fun UpdateCard(onOpenUpdater: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val autoUpdateEnabled = remember(prefs) { prefs.getBoolean("auto_update_app", true) }
    val isSpoofed = remember { isCurrentAppSpoofed() }
    val buildTypeLabel = if (isSpoofed) "Spoofed" else "Normal"

    val (currentVersionName, currentVersionCode) = remember(context) { getManagerVersion(context) }

    val newVersion by produceState(initialValue = LatestVersionInfo(isSpoofed = isSpoofed)) {
        value = withContext(Dispatchers.IO) {
            checkNewVersion(preferSpoofed = isSpoofed)
        }
    }

    val hasUpdate = remember(currentVersionName, currentVersionCode, newVersion) {
        isUpdateAvailable(currentVersionName, currentVersionCode, newVersion)
    }

    var autoUpdating by remember { mutableStateOf(false) }
    var autoUpdateText by remember { mutableStateOf<String?>(null) }

    // Automatically download & install if a strictly newer release (same build type) is found
    LaunchedEffect(hasUpdate, newVersion.versionTag, autoUpdateEnabled) {
        if (hasUpdate && autoUpdateEnabled && lastAutoUpdatedTag != newVersion.versionTag) {
            lastAutoUpdatedTag = newVersion.versionTag
            autoUpdating = true
            autoUpdateText = "Downloading ${newVersion.fileName}…"
            val dlResult = downloadUpdateApk(context, newVersion) { progress, _, _ ->
                if (progress >= 0f) {
                    autoUpdateText = "Downloading ${newVersion.versionTag} (${(progress * 100).toInt()}%)…"
                }
            }
            dlResult.onSuccess { apkFile ->
                autoUpdateText = "Installing ${newVersion.versionTag}…"
                val installResult = installDownloadedApk(context, apkFile)
                autoUpdateText = installResult.getOrNull() ?: "Install failed: ${installResult.exceptionOrNull()?.message}"
            }.onFailure { err ->
                autoUpdateText = "Auto-update failed: ${err.message}"
            }
            autoUpdating = false
        }
    }

    AnimatedVisibility(
        visible = hasUpdate,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenUpdater() },
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(end = 20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            id = R.string.new_version_available,
                            newVersion.versionTag,
                            newVersion.versionCode
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = autoUpdateText ?: "Tap to open App Updater ($buildTypeLabel build)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
                if (autoUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdaterLinkCard(onClick: () -> Unit) {
    val context = LocalContext.current
    val (currentVersionName, _) = remember(context) { getManagerVersion(context) }
    val buildTypeLabel = stringResource(if (isCurrentAppSpoofed()) R.string.updater_build_spoofed else R.string.updater_build_normal)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .gombokPressBounce()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.padding(end = 20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.updater_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.updater_home_summary, currentVersionName, buildTypeLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RebootDropdownItem(@StringRes id: Int, reason: String = "") {
    DropdownMenuItem(text = {
        Text(stringResource(id))
    }, onClick = {
        reboot(reason)
    })
}

// @Composable
// fun getSeasonalIcon(): ImageVector {
//     val month = Calendar.getInstance().get(Calendar.MONTH) // 0-11 for January-December
//     return when (month) {
//         Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> Icons.Filled.AcUnit // Winter
//         Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Icons.Filled.Spa // Spring
//         Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> Icons.Filled.WbSunny // Summer
//         Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> Icons.Filled.Forest // Fall
//         else -> Icons.Filled.Whatshot // Fallback icon
//     }
// }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    kernelVersion: KernelVersion,
    ksuVersion: Int?,
    customTitle: String,
    customSubtitle: String,
    logoResId: Int,
    onInstallClick: () -> Unit,
    onSettingsClick: () -> Unit,
    navBarEnabled: Boolean,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    var isSpinning by remember { mutableStateOf(false) }
    var rotationTarget by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = rotationTarget,
        animationSpec = tween(
            durationMillis = 1400,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        finishedListener = {
            isSpinning = false
        }
    )

    val moduleViewModel: ModuleViewModel = viewModel()
    
    val webUILauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    val context = LocalContext.current
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!isSpinning) {
                        isSpinning = true
                        rotationTarget += 360f * 6
                    }
                }
            ) {
                Icon(
                    painter = painterResource(logoResId),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(end = 8.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                        }
                )
                Column {
                    Text(
                        text = customTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    if (customSubtitle.isNotBlank()) {
                        Text(
                            text = customSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        actions = {
            if (!navBarEnabled) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(id = R.string.settings)
                    )
                }
            }

            if (ksuVersion != null) {
                IconButton(onClick = onInstallClick) {
                    Icon(
                        imageVector = Icons.Filled.Archive,
                        contentDescription = stringResource(id = R.string.install)
                    )
                }
            }

            if (ksuVersion != null) {
                var showDropdown by remember { mutableStateOf(false) }
                IconButton(onClick = {
                    showDropdown = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = stringResource(id = R.string.reboot)
                    )

                    DropdownMenu(expanded = showDropdown, onDismissRequest = {
                        showDropdown = false
                    }) {
                        RebootDropdownItem(id = R.string.reboot)
                        RebootDropdownItem(id = R.string.reboot_userspace, reason = "soft-reboot")

                        val pm =
                            LocalContext.current.getSystemService(Context.POWER_SERVICE) as PowerManager?
                        @Suppress("DEPRECATION")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true) {
                            RebootDropdownItem(id = R.string.reboot_userspace, reason = "userspace")
                        }
                        RebootDropdownItem(id = R.string.reboot_recovery, reason = "recovery")
                        RebootDropdownItem(id = R.string.reboot_bootloader, reason = "bootloader")
                        RebootDropdownItem(id = R.string.reboot_download, reason = "download")
                        RebootDropdownItem(id = R.string.reboot_edl, reason = "edl")
                    }
                }
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}


@Composable
private fun StatusCard(
    kernelVersionParam: KernelVersion,
    ksuVersionParam: Int?,
    uapiVerParam: Int? = null,
    lkmModeParam: Boolean?,
    moduleUpdateCount: Int = 0,
    ksuVersionTagParam: String? = null,
    onClickInstall: () -> Unit = {}
) {
    val context = LocalContext.current

    val anim = LocalAnimationConfig.current
    ElevatedCard(
        modifier = Modifier.gombokPressBounce(),
        colors = CardDefaults.elevatedCardColors(containerColor = run {
            if (ksuVersionParam != null) MaterialTheme.colorScheme.primary
            else if (kernelVersionParam.isGKI()) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer
        })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (ksuVersionParam == null) {
                        onClickInstall()
                    }
                }
                .padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                ksuVersionParam != null -> {
                    val workingMode = if (lkmModeParam == true || lkmModeParam == false) {
                        val mode = if (lkmModeParam == true) "LKM" else "BUILT-IN"
                        "$mode (" + kernelVersionParam.getKernelType() + ")"
                    } else kernelVersionParam.getKernelType()

                    Box(contentAlignment = Alignment.Center) {
                        if (anim.enabled && anim.statusPulseGlow) {
                            val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.65f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = anim.scaledDuration(1800),
                                        easing = FastOutSlowInEasing
                                    ),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "pulseScale"
                            )
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.45f,
                                targetValue = 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = anim.scaledDuration(1800),
                                        easing = FastOutSlowInEasing
                                    ),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "pulseAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                        alpha = pulseAlpha
                                    }
                                    .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Mood,
                            contentDescription = null
                        )
                    }
                    Column(
                        modifier = Modifier.padding(start = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val labelStyle = LabelItemDefaults.style
                        TextRow(
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LabelItem(
                                        icon = if (Natives.isSafeMode) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.Security,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.VerifiedUser,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        text = {
                                            Text(
                                                text = workingMode,
                                                style = labelStyle.textStyle.copy(
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                )
                                            )
                                        },
                                        style = LabelItemDefaults.style.copy(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                    if (isSuCompatDisabled()) {
                                        LabelItem(
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Warning,
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    contentDescription = null
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.sucompat_disabled),
                                                    style = labelStyle.textStyle.copy(
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    )
                                                )
                                            },
                                            style = LabelItemDefaults.style.copy(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                        )
                                    }
                                    if (Natives.isLateLoadMode) {
                                        LabelItem(
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Warning,
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    contentDescription = null
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.jailbreak_mode),
                                                    style = labelStyle.textStyle.copy(
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                    )
                                                )
                                            },
                                            style = LabelItemDefaults.style.copy(
                                                containerColor = MaterialTheme.colorScheme.errorContainer
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_working),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val ksuVer = ksuVersionParam ?: 0
                        val uapiVer = uapiVerParam ?: 0
                        val tag = if (!ksuVersionTagParam.isNullOrEmpty()) ksuVersionTagParam else "v0.0.0"
                        val versionText = stringResource(
                            R.string.home_working_version,
                            tag,
                            "$ksuVer-$uapiVer"
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = versionText,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (lkmModeParam == true && !Natives.isLkmBundled) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.home_lkm_custom),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                kernelVersionParam.isGKI() -> {
                    Icon(Icons.Filled.AutoFixHigh, null)
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(
                            text = stringResource(R.string.home_not_installed),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_click_to_install),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    Icon(Icons.Filled.MoodBad, null)
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(
                            text = stringResource(R.string.home_failure),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_failure_tip),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WarningCard(
    message: String, color: Color = MaterialTheme.colorScheme.error, onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = color
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.SentimentDissatisfied,
                contentDescription = null,
                modifier = Modifier.padding(end = 20.dp)
            )
            Text(
                text = message, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private data class HomeInfoSnapshot(
    val managerVersion: Pair<String, Long> = "" to 0L,
    val managerUAPIVersion: Int = 0,
    val managerAppId: Int = 0,
    val hookMode: String? = null,
    val metaModule: String? = null,
    val metaInfo: ModuleViewModel.ModuleInfo? = null,
    val suSFS: String? = null,
    val suSFSVersion: String? = null,
    val suSFSVariant: String? = null,
    val zygiskEnabled: Boolean = false,
    val zygiskInfo: ModuleViewModel.ModuleInfo? = null,
    val deviceModel: String = "",
    val fingerprint: String = Build.FINGERPRINT,
    val unameRelease: String = "",
    val unameMachine: String = "",
    val seccompStatus: String = "Unavailable",
    val selinuxStatusRes: Int = R.string.selinux_status_unknown,
)

private fun getSystemProperty(key: String): String {
    return runCatching {
        val spClass = Class.forName("android.os.SystemProperties")
        val getMethod = spClass.getMethod("get", String::class.java, String::class.java)
        (getMethod.invoke(null, key, "") as? String)?.trim().orEmpty()
    }.getOrDefault("")
}

private fun getDeviceModelName(context: Context): String {
    val manufacturerRaw = Build.MANUFACTURER.orEmpty().trim()
    val manufacturer = manufacturerRaw.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
    }
    val model = Build.MODEL.orEmpty().trim()

    val marketName = listOf(
        "ro.product.marketname",
        "ro.product.odm.marketname",
        "ro.product.vendor.marketname",
        "ro.config.marketing_name",
        "ro.vendor.oplus.market.name",
        "ro.oppo.market.name"
    ).firstNotNullOfOrNull { prop ->
        getSystemProperty(prop).takeIf { it.isNotBlank() }
    } ?: run {
        val settingsName = runCatching {
            android.provider.Settings.Global.getString(context.contentResolver, "device_name")?.trim()
        }.getOrNull().orEmpty()
        if (manufacturerRaw.equals("samsung", ignoreCase = true) && settingsName.contains("Galaxy", ignoreCase = true)) {
            settingsName
        } else {
            ""
        }
    }

    val baseName = when {
        marketName.isNotBlank() -> {
            if (manufacturer.isNotBlank() && !marketName.startsWith(manufacturer, ignoreCase = true)) {
                "$manufacturer $marketName"
            } else {
                marketName
            }
        }
        manufacturer.isNotBlank() && model.isNotBlank() -> {
            if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        }
        model.isNotBlank() -> model
        else -> manufacturer.ifBlank { "Unknown" }
    }

    return if (model.isNotBlank() && !baseName.contains(model, ignoreCase = true)) {
        "$baseName ($model)"
    } else {
        baseName
    }
}

private fun buildHomeInfoSnapshot(
    context: Context,
    ksuVersion: Int?,
    moduleList: List<ModuleViewModel.ModuleInfo>,
): HomeInfoSnapshot {
    val managerVersion = getManagerVersion(context)
    val managerUAPIVersion = Natives.managerUAPIVersion
    val managerAppId = Natives.getManagerAppid()
    val hookMode = if (ksuVersion == null) null else {
        Natives.getHookMode().takeUnless { it.isNullOrBlank() } ?: "Unavailable"
    }
    val metaModule = if (ksuVersion == null) null else getMetaModule()
    val metaInfo = if (metaModule == null) null else moduleList.firstOrNull { it.isMetaModule }
    val suSFS = if (ksuVersion == null) null else getSuSFS()
    val suSFSVersion = if (suSFS == "Supported" && ksuVersion != null) getSuSFSVersion() else null
    val suSFSVariant = if (suSFS == "Supported" && ksuVersion != null) getSuSFSVariant() else null
    val zygiskEnabled = if (ksuVersion == null) false else Natives.isZygiskEnabled()
    val zygiskInfo = if (!zygiskEnabled) null else moduleList.firstOrNull { it.isZygisk && it.enabled }
    val deviceModel = getDeviceModelName(context)
    val fingerprint = Build.FINGERPRINT.orEmpty().ifBlank { "Unknown" }
    val uname = kotlin.runCatching { Os.uname() }.getOrNull()
    val statusInt = kotlin.runCatching { Os.prctl(21, 0, 0, 0, 0) }.getOrDefault(-1)
    val seccompStatus = when (statusInt) {
        -1 -> "Unavailable"
        0 -> "Disabled"
        1 -> "Strict"
        2 -> "Filter"
        else -> "Unknown"
    }

    return HomeInfoSnapshot(
        managerVersion = managerVersion,
        managerUAPIVersion = managerUAPIVersion,
        managerAppId = managerAppId,
        hookMode = hookMode,
        metaModule = metaModule,
        metaInfo = metaInfo,
        suSFS = suSFS,
        suSFSVersion = suSFSVersion,
        suSFSVariant = suSFSVariant,
        zygiskEnabled = zygiskEnabled,
        zygiskInfo = zygiskInfo,
        deviceModel = deviceModel,
        fingerprint = fingerprint,
        unameRelease = uname?.release.orEmpty(),
        unameMachine = uname?.machine.orEmpty(),
        seccompStatus = seccompStatus,
        selinuxStatusRes = getSELinuxStatus(),
    )
}

@Composable
private fun InfoCard(autoExpand: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    val isManager = remember { Natives.isManager }
    val ksuVersion = remember(isManager) { if (isManager) Natives.version else null }

    var expanded by rememberSaveable { mutableStateOf(autoExpand) }
    val developerOptionsEnabled = remember(prefs) { prefs.getBoolean("enable_developer_options", false) }

    LaunchedEffect(autoExpand) {
        expanded = autoExpand
    }

    val moduleViewModel: ModuleViewModel = viewModel()
    var homeInfo by remember { mutableStateOf(HomeInfoSnapshot()) }

    LaunchedEffect(ksuVersion, moduleViewModel.moduleList) {
        homeInfo = withContext(Dispatchers.IO) {
            buildHomeInfoSnapshot(context, ksuVersion, moduleViewModel.moduleList)
        }
    }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            @Composable
            fun InfoCardItem(label: String, content: String, icon: Any? = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        when (icon) {
                            is ImageVector -> Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 20.dp)
                            )
                            is Painter -> Icon(
                                painter = icon,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Column {
                InfoCardItem(
                    label = stringResource(R.string.home_manager_version),
                    content = if (developerOptionsEnabled) {
                        "${homeInfo.managerVersion.first} (${homeInfo.managerVersion.second}-${homeInfo.managerUAPIVersion}) | UID: ${homeInfo.managerAppId}"
                    } else {
                        "${homeInfo.managerVersion.first} (${homeInfo.managerVersion.second}-${homeInfo.managerUAPIVersion})"
                    },
                    icon = Icons.Filled.AutoAwesomeMotion,
                )

                if (ksuVersion != null) {
                    Spacer(Modifier.height(16.dp))
                    InfoCardItem(
                        label = stringResource(R.string.hook_mode),
                        content = homeInfo.hookMode ?: stringResource(R.string.unavailable),
                        icon = Icons.Filled.Phishing,
                    )
                }

                if (ksuVersion != null) {
                    val metaInfo = homeInfo.metaInfo
                    val metaDetail = if (metaInfo != null) " | ${metaInfo.name} | ${metaInfo.version}" else ""
                    Spacer(Modifier.height(16.dp))
                    InfoCardItem(
                        label = stringResource(R.string.home_metamodule_status),
                        content = when {
                            homeInfo.metaModule == "Installed" && metaInfo != null && !metaInfo.enabled ->
                                stringResource(R.string.disabled) + metaDetail
                            homeInfo.metaModule == "Installed" ->
                                stringResource(R.string.installed) + metaDetail
                            else ->
                                stringResource(R.string.home_not_installed)
                        },
                        icon = Icons.Filled.SettingsSuggest
                    )

                    if (homeInfo.suSFS == "Supported") {
                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_susfs_version),
                            content = "${stringResource(R.string.supported)} | ${homeInfo.suSFSVersion ?: stringResource(R.string.unavailable)} (${homeInfo.suSFSVariant ?: stringResource(R.string.unavailable)})",
                            icon = painterResource(R.drawable.ic_sus),
                        )
                    }

                    if (homeInfo.zygiskEnabled) {
                        Spacer(Modifier.height(16.dp))
                        val zygiskInfo = homeInfo.zygiskInfo
                        val zygiskDetail = if (zygiskInfo != null) " | ${zygiskInfo.name} | ${zygiskInfo.version}" else ""
                        InfoCardItem(
                            label = stringResource(R.string.zygisk_status),
                            content = stringResource(R.string.enabled) + zygiskDetail,
                            icon = Icons.Filled.Vaccines
                        )
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_device_model),
                            content = homeInfo.deviceModel,
                            icon = Icons.Filled.PhoneAndroid,
                        )

                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_fingerprint),
                            content = homeInfo.fingerprint,
                            icon = Icons.Filled.Fingerprint,
                        )

                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_kernel),
                            content = "${homeInfo.unameRelease} (${homeInfo.unameMachine})",
                            icon = painterResource(R.drawable.ic_linux),
                        )

                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_android),
                            content = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
                            icon = Icons.Filled.Android,
                        )

                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_abi),
                            content = Build.SUPPORTED_ABIS.joinToString(", "),
                            icon = Icons.Filled.Memory,
                        )

                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_selinux_status),
                            content = stringResource(homeInfo.selinuxStatusRes),
                            icon = Icons.Filled.Security,
                        )

                        Spacer(Modifier.height(16.dp))
                        InfoCardItem(
                            label = stringResource(R.string.home_seccomp_status),
                            content = homeInfo.seccompStatus,
                            icon = Icons.Filled.LocalPolice
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        animationSpec = tween(durationMillis = 300)
                    )

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded) null else null,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = rotationAngle
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContributorsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .gombokPressBounce()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Groups,
                contentDescription = null,
                modifier = Modifier.padding(end = 20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.contributors),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.home_contributors_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun IssueReportCard() {
    val uriHandler = LocalUriHandler.current
    val githubIssueUrl = stringResource(R.string.issue_report_github_link)

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.issue_report_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.issue_report_body),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.issue_report_body_2),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = { uriHandler.openUri(githubIssueUrl) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = stringResource(R.string.issue_report_github),
                    )
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
fun handleDynamicShortcuts(context: Context, moduleConfigs: List <Pair<ModuleViewModel.ModuleInfo, Int>>) {
    ShortcutManagerCompat.removeAllDynamicShortcuts(context)

    moduleConfigs.forEach { (module, iconRes) ->
        val shortcut = ShortcutInfoCompat.Builder(context, module.id)
            .setShortLabel(module.name)
            .setLongLabel(module.name)
            .setIcon(IconCompat.createWithResource(context, iconRes))
            .setCategories(setOf(ShortcutInfo.SHORTCUT_CATEGORY_CONVERSATION))
            .setIntent(
                Intent(context, WebUIActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = "kernelsu://webui/${module.id}".toUri()
                    putExtra("id", module.id)
                    putExtra("name", module.name)
                }
            )
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }
}

fun getManagerVersion(context: Context): Pair<String, Long> {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)!!
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return Pair(packageInfo.versionName!!, versionCode)
}

@Preview
@Composable
private fun StatusCardPreview() {
    Column {
        StatusCard(KernelVersion(5, 10, 101), 1, 1, null)
        StatusCard(KernelVersion(5, 10, 101), 20000, 1, true)
        StatusCard(KernelVersion(5, 10, 101), null, null, true)
        StatusCard(KernelVersion(4, 10, 101), null, null, false)
    }
}

@Preview
@Composable
private fun WarningCardPreview() {
    Column {
        WarningCard(message = "Warning message")
        WarningCard(
            message = "Warning message",
            MaterialTheme.colorScheme.outlineVariant,
            onClick = {})
    }
}
