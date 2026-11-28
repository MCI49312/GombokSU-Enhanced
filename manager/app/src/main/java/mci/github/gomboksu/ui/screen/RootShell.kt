package mci.github.gomboksu.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.topjohnwu.superuser.CallbackList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mci.github.gomboksu.R
import mci.github.gomboksu.ksuApp
import mci.github.gomboksu.ui.LocalNavBarEnabled
import mci.github.gomboksu.ui.LocalScrollState
import mci.github.gomboksu.ui.rememberScrollConnection
import mci.github.gomboksu.ui.util.createRootShell
import mci.github.gomboksu.ui.util.gombokPressBounce
import mci.github.gomboksu.ui.util.rootAvailable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DiagnosticPreset(
    val titleRes: Int,
    val command: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun RootShellScreen(navigator: DestinationsNavigator) {
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

    val hasRoot = remember { rootAvailable() }
    val ksudPath = remember {
        ksuApp.applicationInfo.nativeLibraryDir + File.separator + "libksud.so"
    }

    val presets = remember(ksudPath) {
        listOf(
            DiagnosticPreset(R.string.root_shell_preset_identity, "id && uname -a && getenforce"),
            DiagnosticPreset(R.string.root_shell_preset_ksu_modules, "$ksudPath module list"),
            DiagnosticPreset(R.string.root_shell_preset_dmesg, "dmesg | tail -n 120"),
            DiagnosticPreset(R.string.root_shell_preset_logcat, "logcat -d -v time *:W | tail -n 120"),
            DiagnosticPreset(R.string.root_shell_preset_mounts, "mount | grep -E 'adb|ksu|overlay|apatch|tmpfs' | head -n 120"),
            DiagnosticPreset(
                R.string.root_shell_preset_props,
                "for p in ro.product.model ro.build.version.release ro.build.fingerprint ro.boot.vbmeta.device_state ro.boot.flash.locked sys.boot_completed; do echo \"\$p=$(getprop \$p)\"; done"
            ),
            DiagnosticPreset(
                R.string.root_shell_preset_susfs,
                "$ksudPath -V 2>&1; ls -la /data/adb /data/adb/modules 2>&1"
            ),
            DiagnosticPreset(R.string.root_shell_preset_processes, "ps -A -o PID,USER,NAME | head -n 100")
        )
    }

    var commandInput by rememberSaveable { mutableStateOf("id && uname -r && getenforce") }
    var globalMountNs by rememberSaveable { mutableStateOf(true) }
    var isRunning by remember { mutableStateOf(false) }
    var terminalOutput by remember { mutableStateOf("") }
    var lastExitCode by remember { mutableStateOf<Int?>(null) }
    var lastDurationMs by remember { mutableStateOf<Long?>(null) }
    val commandHistory = remember { mutableStateListOf<String>() }

    val consoleVerticalScroll = rememberScrollState()
    val consoleHorizontalScroll = rememberScrollState()

    val saveOutputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(terminalOutput.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.log_saved, Toast.LENGTH_SHORT).show()
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.root_shell_save_failed, err.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun executeCommand(rawCmd: String) {
        val cmd = rawCmd.trim()
        if (cmd.isEmpty() || isRunning) return
        commandInput = cmd

        commandHistory.remove(cmd)
        commandHistory.add(0, cmd)
        if (commandHistory.size > 10) {
            commandHistory.removeAt(commandHistory.lastIndex)
        }

        scope.launch {
            isRunning = true
            lastExitCode = null
            lastDurationMs = null
            val sb = StringBuilder()
            sb.append("# ").append(cmd).append('\n')
            terminalOutput = sb.toString()

            val startMs = System.currentTimeMillis()
            val exitCode = withContext(Dispatchers.IO) {
                val liveCallback = object : CallbackList<String>(java.util.Collections.synchronizedList(mutableListOf())) {
                    override fun onAddElement(e: String?) {
                        if (e != null) {
                            synchronized(sb) {
                                sb.append(e).append('\n')
                            }
                            val snapshot = synchronized(sb) { sb.toString() }
                            scope.launch(Dispatchers.Main) {
                                terminalOutput = snapshot
                            }
                        }
                    }
                }

                runCatching {
                    val shell = createRootShell(globalMountNs)
                    shell.use { sh ->
                        val res = sh.newJob()
                            .add(cmd)
                            .to(liveCallback, liveCallback)
                            .exec()
                        res.code
                    }
                }.getOrElse { err ->
                    synchronized(sb) {
                        sb.append("Error: ").append(err.message ?: err.javaClass.simpleName).append('\n')
                    }
                    -1
                }
            }

            lastDurationMs = System.currentTimeMillis() - startMs
            lastExitCode = exitCode
            val finalStr = synchronized(sb) {
                if (sb.lines().size <= 2) {
                    sb.append(context.getString(R.string.root_shell_no_output)).append('\n')
                }
                sb.toString()
            }
            terminalOutput = finalStr
            isRunning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.root_shell_title),
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
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("GombokSU Shell", terminalOutput))
                            Toast.makeText(context, R.string.root_shell_copied, Toast.LENGTH_SHORT).show()
                        },
                        enabled = terminalOutput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = stringResource(R.string.root_shell_copy_output)
                        )
                    }
                    IconButton(
                        onClick = {
                            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            saveOutputLauncher.launch("gomboksu_shell_$ts.txt")
                        },
                        enabled = terminalOutput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = stringResource(R.string.root_shell_save_output)
                        )
                    }
                    IconButton(
                        onClick = {
                            terminalOutput = ""
                            lastExitCode = null
                            lastDurationMs = null
                        },
                        enabled = !isRunning && terminalOutput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.root_shell_clear_output)
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
                .fillMaxSize()
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = navBarPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Root Environment Card + Global Mount Namespace toggle
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (hasRoot) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (hasRoot) Icons.Filled.Terminal else Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = if (hasRoot) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    if (hasRoot) R.string.root_shell_root_ready else R.string.root_shell_no_root
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(
                                    if (hasRoot) R.string.root_shell_root_ready_summary else R.string.root_shell_no_root_summary
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isRunning) { globalMountNs = !globalMountNs }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.root_shell_global_mnt),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.root_shell_global_mnt_summary),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = globalMountNs,
                            enabled = !isRunning,
                            onCheckedChange = { globalMountNs = it }
                        )
                    }
                }
            }

            // One-Tap Quick Diagnostics
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.root_shell_quick_diagnostics),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets, key = { it.titleRes }) { preset ->
                        AssistChip(
                            onClick = { executeCommand(preset.command) },
                            enabled = !isRunning,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(stringResource(preset.titleRes)) }
                        )
                    }
                }
            }

            // Command Input Row
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        label = { Text(stringResource(R.string.root_shell_command_label)) },
                        placeholder = { Text(stringResource(R.string.root_shell_command_placeholder)) },
                        prefix = {
                            Text(
                                text = if (hasRoot) "# " else "$ ",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        enabled = !isRunning,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { executeCommand(commandInput) }),
                        trailingIcon = {
                            if (commandInput.isNotEmpty() && !isRunning) {
                                IconButton(onClick = { commandInput = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { executeCommand(commandInput) },
                            enabled = !isRunning && commandInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .gombokPressBounce(enabled = !isRunning && commandInput.isNotBlank()),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    if (isRunning) R.string.root_shell_running else R.string.root_shell_run
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (commandHistory.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(commandHistory, key = { it }) { histCmd ->
                                SuggestionChip(
                                    onClick = { commandInput = histCmd },
                                    label = {
                                        Text(
                                            text = histCmd,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.widthIn(max = 180.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Terminal Console Output
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1116))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF171C24))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.root_shell_console_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                        val statusText = when {
                            isRunning -> stringResource(R.string.root_shell_running)
                            lastExitCode != null -> stringResource(
                                R.string.root_shell_exit_status,
                                lastExitCode ?: 0,
                                lastDurationMs ?: 0L
                            )
                            else -> stringResource(R.string.root_shell_idle)
                        }
                        val statusColor = when {
                            isRunning -> Color(0xFFFBBF24)
                            lastExitCode == 0 -> Color(0xFF4ADE80)
                            lastExitCode != null -> Color(0xFFF87171)
                            else -> Color(0xFF94A3B8)
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = statusColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp, max = 440.dp)
                            .verticalScroll(consoleVerticalScroll)
                            .horizontalScroll(consoleHorizontalScroll)
                            .padding(14.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = terminalOutput.ifEmpty {
                                    stringResource(R.string.root_shell_empty_hint)
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = if (terminalOutput.isEmpty()) {
                                    Color(0xFF64748B)
                                } else {
                                    Color(0xFFE2E8F0)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
