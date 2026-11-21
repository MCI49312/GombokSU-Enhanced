package mci.github.gomboksu.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import mci.github.gomboksu.ui.LocalNavBarEnabled
import mci.github.gomboksu.ui.LocalScrollState
import mci.github.gomboksu.ui.component.SwitchItem
import mci.github.gomboksu.ui.rememberScrollConnection
import mci.github.gomboksu.ui.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CustomSpoofScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    val navBarEnabled = LocalNavBarEnabled.current
    val isNavBarHidden = (bottomBarScrollState?.isScrollingDown?.value ?: false) || (navBarEnabled?.value == false)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (isNavBarHidden) 0.dp else 112.dp

    val currentPackageId = remember(context) { context.packageName }
    val currentAppName = remember(context) {
        runCatching {
            context.applicationInfo.loadLabel(context.packageManager).toString()
        }.getOrDefault("GombokSU Enhanced")
    }
    val hasRoot = remember { rootAvailable() }

    var customPackageId by rememberSaveable { mutableStateOf(ApkSpoofer.generateRandomPackageId()) }
    var customAppName by rememberSaveable { mutableStateOf(currentAppName) }
    var uninstallOldAfterInstall by rememberSaveable { mutableStateOf(hasRoot) }

    var isWorking by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    val pkgValid = ApkSpoofer.isValidPackageId(customPackageId.trim())
    val nameValid = customAppName.trim().isNotEmpty() && customAppName.trim().length <= 64
    val canBuild = pkgValid && nameValid && !isWorking

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Custom Spoof",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
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
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
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
                .padding(top = 12.dp)
                .padding(bottom = navBarPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Identity Card
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Spoofed Identity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "App Name: $currentAppName\nPackage ID: $currentPackageId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Input Form Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "New Custom Identity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = customAppName,
                        onValueChange = { customAppName = it.take(64) },
                        label = { Text("Custom App Name") },
                        placeholder = { Text("e.g. Notes, Calculator, System Tools") },
                        singleLine = true,
                        enabled = !isWorking,
                        isError = !nameValid,
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                        supportingText = {
                            Text(
                                if (nameValid) "Shown on your launcher and in Android App Info"
                                else "App name cannot be empty"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customPackageId,
                        onValueChange = { customPackageId = it.lowercase().filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == '.' || ch == '_' } },
                        label = { Text("Custom Package ID") },
                        placeholder = { Text("e.g. com.my.notes") },
                        singleLine = true,
                        enabled = !isWorking,
                        isError = !pkgValid,
                        leadingIcon = { Icon(Icons.Filled.Android, contentDescription = null) },
                        trailingIcon = {
                            IconButton(
                                onClick = { customPackageId = ApkSpoofer.generateRandomPackageId() },
                                enabled = !isWorking
                            ) {
                                Icon(Icons.Filled.Shuffle, contentDescription = "Generate random package ID")
                            }
                        },
                        supportingText = {
                            Text(
                                if (pkgValid) "Valid Android package ID (re-signed with 0x3e6 GombokSU key)"
                                else "Use lowercase letters, digits, underscores, and at least one dot (e.g. com.my.app)"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (hasRoot) {
                        SwitchItem(
                            icon = Icons.Filled.DeleteSweep,
                            title = "Remove Old Spoofed App",
                            summary = "Automatically uninstall $currentPackageId after launching the new package",
                            checked = uninstallOldAfterInstall,
                            enabled = !isWorking,
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) { checked ->
                            uninstallOldAfterInstall = checked
                        }
                    }
                }
            }

            // Progress / Status Card
            AnimatedVisibility(visible = isWorking || statusText != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = statusText.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (isWorking) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // Action Buttons
            Button(
                onClick = {
                    scope.launch {
                        isWorking = true
                        statusText = "Starting custom spoof build…"
                        val buildRes = ApkSpoofer.buildCustomSpoofedApk(
                            context = context,
                            newPackageId = customPackageId,
                            newAppName = customAppName,
                            onStatus = { msg -> scope.launch { statusText = msg } }
                        )
                        buildRes.onSuccess { signedApk ->
                            val installRes = ApkSpoofer.installCustomSpoofedApk(
                                context = context,
                                signedApk = signedApk,
                                newPackageId = customPackageId.trim(),
                                uninstallOld = uninstallOldAfterInstall,
                                onStatus = { msg -> scope.launch { statusText = msg } }
                            )
                            installRes.onSuccess { msg ->
                                statusText = msg
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }.onFailure { err ->
                                val errMsg = "Install failed: ${err.message}"
                                statusText = errMsg
                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                            }
                        }.onFailure { err ->
                            val errMsg = "Repackage failed: ${err.message}"
                            statusText = errMsg
                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        }
                        isWorking = false
                    }
                },
                enabled = canBuild,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .gombokPressBounce(enabled = canBuild),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Build & Install Custom Spoofed APK", fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        isWorking = true
                        statusText = "Building custom spoofed APK…"
                        val buildRes = ApkSpoofer.buildCustomSpoofedApk(
                            context = context,
                            newPackageId = customPackageId,
                            newAppName = customAppName,
                            onStatus = { msg -> scope.launch { statusText = msg } }
                        )
                        buildRes.onSuccess { signedApk ->
                            val saveRes = ApkSpoofer.copyApkToDownloads(signedApk)
                            saveRes.onSuccess { outFile ->
                                val msg = "Saved to Downloads/${outFile.name}"
                                statusText = msg
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }.onFailure { err ->
                                val errMsg = "Failed to save to Downloads: ${err.message}"
                                statusText = errMsg
                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                            }
                        }.onFailure { err ->
                            val errMsg = "Repackage failed: ${err.message}"
                            statusText = errMsg
                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        }
                        isWorking = false
                    }
                },
                enabled = canBuild,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .gombokPressBounce(enabled = canBuild),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Custom APK to Downloads", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
