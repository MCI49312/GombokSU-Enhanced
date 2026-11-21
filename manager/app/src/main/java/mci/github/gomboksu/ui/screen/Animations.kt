package mci.github.gomboksu.ui.screen

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import mci.github.gomboksu.ui.LocalNavBarEnabled
import mci.github.gomboksu.ui.LocalScrollState
import mci.github.gomboksu.ui.MainActivity
import mci.github.gomboksu.ui.component.SwitchItem
import mci.github.gomboksu.ui.rememberScrollConnection
import mci.github.gomboksu.ui.util.*
import java.util.Locale
import kotlin.math.roundToInt

private data class AnimationPreset(
    val name: String,
    val bounce: Float,
    val speed: Float,
    val transition: Int,
    val splash: Int
)

private val ANIMATION_PRESETS = listOf(
    AnimationPreset("Balanced", 1.0f, 1.0f, 1, 1),
    AnimationPreset("Snappy", 0.5f, 1.7f, 0, 1),
    AnimationPreset("Bouncy", 1.6f, 0.95f, 1, 1),
    AnimationPreset("Jelly", 2.4f, 0.75f, 4, 2),
    AnimationPreset("Cinematic", 0.8f, 0.55f, 2, 3),
    AnimationPreset("Minimal", 0.0f, 1.8f, 0, 0)
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AnimationsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? MainActivity }
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

    val navBarEnabled = LocalNavBarEnabled.current
    val isNavBarHidden = (bottomBarScrollState?.isScrollingDown?.value ?: false) || (navBarEnabled?.value == false)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (isNavBarHidden) 0.dp else 112.dp

    var config by remember { mutableStateOf(AnimationConfig.fromPrefs(prefs)) }
    var enableStartupAnimation by rememberSaveable {
        mutableStateOf(prefs.getBoolean("enable_startup_animation", true))
    }

    fun updateConfig(newConfig: AnimationConfig) {
        config = newConfig
        activity?.updateAnimationConfig(newConfig)
    }

    // Live interactive playground state
    var playgroundToggle by remember { mutableStateOf(false) }
    var previewingSplash by remember { mutableStateOf(false) }

    val playgroundScale by animateFloatAsState(
        targetValue = if (playgroundToggle) 1.18f else 1.0f,
        animationSpec = config.springSpec(stiffnessScale = 1.0f),
        label = "playgroundScale"
    )
    val playgroundRotation by animateFloatAsState(
        targetValue = if (playgroundToggle) 180f else 0f,
        animationSpec = config.springSpec(stiffnessScale = 0.85f),
        label = "playgroundRotation"
    )
    val playgroundOffsetX by animateFloatAsState(
        targetValue = if (playgroundToggle) 38f else -38f,
        animationSpec = config.springSpec(stiffnessScale = 0.9f),
        label = "playgroundOffset"
    )

    val bounceLabel = when {
        !config.enabled || config.bounceMultiplier <= 0.05f -> "No Bounce"
        config.bounceMultiplier < 0.7f -> "Subtle"
        config.bounceMultiplier < 1.3f -> "Normal"
        config.bounceMultiplier < 1.9f -> "Bouncy"
        config.bounceMultiplier < 2.3f -> "Extra Bouncy"
        else -> "Jelly!"
    }

    val speedLabel = when {
        config.speedMultiplier < 0.65f -> "Slow-Mo"
        config.speedMultiplier < 0.9f -> "Relaxed"
        config.speedMultiplier < 1.25f -> "Normal"
        config.speedMultiplier < 1.8f -> "Snappy"
        else -> "Hyper"
    }

    val transitionOptions = listOf(
        0 to "Slide",
        1 to "Bouncy Zoom",
        2 to "Depth Push",
        3 to "Vertical Rise",
        4 to "Elastic Pop"
    )

    val splashOptions = listOf(
        0 to "Classic Spin",
        1 to "Elastic Pop",
        2 to "Pulse Wave",
        3 to "3D Flip"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Animations",
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
                    actions = {
                        IconButton(onClick = {
                            val defaults = AnimationConfig()
                            updateConfig(defaults)
                            enableStartupAnimation = true
                            prefs.edit { putBoolean("enable_startup_animation", true) }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.RestartAlt,
                                contentDescription = "Reset animations"
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
                // LIVE INTERACTIVE PLAYGROUND CARD
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .gombokPressBounce()
                        .clickable { playgroundToggle = !playgroundToggle }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Physics Playground",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "Tap to Bounce",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = playgroundOffsetX * density
                                        scaleX = playgroundScale
                                        scaleY = playgroundScale
                                        rotationZ = playgroundRotation
                                    }
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Text(
                            text = String.format(
                                Locale.US,
                                "Bounce: %.1fx (%s)  •  Speed: %.1fx (%s)",
                                config.bounceMultiplier,
                                bounceLabel,
                                config.speedMultiplier,
                                speedLabel
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }

                // QUICK PRESETS
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Quick Presets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ANIMATION_PRESETS, key = { it.name }) { preset ->
                                val isSelected =
                                    kotlin.math.abs(config.bounceMultiplier - preset.bounce) < 0.06f &&
                                        kotlin.math.abs(config.speedMultiplier - preset.speed) < 0.06f &&
                                        config.pageTransitionStyle == preset.transition
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        updateConfig(
                                            config.copy(
                                                enabled = true,
                                                bounceMultiplier = preset.bounce,
                                                speedMultiplier = preset.speed,
                                                pageTransitionStyle = preset.transition,
                                                splashStyle = preset.splash
                                            )
                                        )
                                        playgroundToggle = !playgroundToggle
                                    },
                                    label = { Text(preset.name) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // PHYSICS ENGINE (BOUNCE MULTIPLIER & SPEED MULTIPLIER)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Spring Physics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        SwitchItem(
                            icon = Icons.Filled.Animation,
                            title = "Enable Custom Animations",
                            summary = "Master toggle for spring physics, bounce effects, and transitions",
                            checked = config.enabled,
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) { checked ->
                            updateConfig(config.copy(enabled = checked))
                        }

                        // Bounce Multiplier Slider
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bounce Multiplier",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1fx • %s", config.bounceMultiplier, bounceLabel),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = config.bounceMultiplier,
                                onValueChange = { raw ->
                                    val snapped = (raw * 10f).roundToInt() / 10f
                                    updateConfig(config.copy(bounceMultiplier = snapped))
                                },
                                onValueChangeFinished = { playgroundToggle = !playgroundToggle },
                                valueRange = 0f..2.5f,
                                steps = 24,
                                enabled = config.enabled
                            )
                        }

                        // Animation Speed Slider
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Animation Speed",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1fx • %s", config.speedMultiplier, speedLabel),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = config.speedMultiplier,
                                onValueChange = { raw ->
                                    val snapped = (raw * 10f).roundToInt() / 10f
                                    updateConfig(config.copy(speedMultiplier = snapped))
                                },
                                onValueChangeFinished = { playgroundToggle = !playgroundToggle },
                                valueRange = 0.4f..2.5f,
                                steps = 20,
                                enabled = config.enabled
                            )
                        }
                    }
                }

                // PAGE TRANSITION STYLE
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Page Transition Style",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(transitionOptions, key = { it.first }) { (styleId, label) ->
                                val selected = config.pageTransitionStyle == styleId
                                FilterChip(
                                    selected = selected,
                                    enabled = config.enabled,
                                    onClick = { updateConfig(config.copy(pageTransitionStyle = styleId)) },
                                    label = { Text(label) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // STARTUP SPLASH ANIMATION
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Startup Splash Animation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(
                                onClick = { previewingSplash = true },
                                enabled = enableStartupAnimation && config.enabled
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Preview")
                            }
                        }

                        SwitchItem(
                            icon = Icons.Filled.AutoAwesome,
                            title = "Play Startup Splash",
                            summary = "Show animated app icon intro when launching GombokSU Enhanced",
                            checked = enableStartupAnimation,
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) { checked ->
                            enableStartupAnimation = checked
                            prefs.edit { putBoolean("enable_startup_animation", checked) }
                        }

                        if (enableStartupAnimation) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(splashOptions, key = { it.first }) { (styleId, label) ->
                                    val selected = config.splashStyle == styleId
                                    FilterChip(
                                        selected = selected,
                                        enabled = config.enabled,
                                        onClick = { updateConfig(config.copy(splashStyle = styleId)) },
                                        label = { Text(label) },
                                        leadingIcon = if (selected) {
                                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }

                // VISUAL MOTION EFFECTS
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Motion Effects",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        SwitchItem(
                            icon = Icons.Filled.TouchApp,
                            title = "Card Press Bounce",
                            summary = "Tactile spring squish & bounce when tapping cards",
                            checked = config.cardPressBounce,
                            enabled = config.enabled,
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) { checked ->
                            updateConfig(config.copy(cardPressBounce = checked))
                        }

                        SwitchItem(
                            icon = Icons.Filled.ViewStream,
                            title = "Staggered Card Entrance",
                            summary = "Cascade cards smoothly into view when opening screens",
                            checked = config.staggeredEntrance,
                            enabled = config.enabled,
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) { checked ->
                            updateConfig(config.copy(staggeredEntrance = checked))
                        }

                        SwitchItem(
                            icon = Icons.Filled.Radar,
                            title = "Status Card Pulse Glow",
                            summary = "Subtle breathing pulse ring on the Home Working status card",
                            checked = config.statusPulseGlow,
                            enabled = config.enabled,
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) { checked ->
                            updateConfig(config.copy(statusPulseGlow = checked))
                        }

                        SwitchItem(
                            icon = Icons.Filled.Navigation,
                            title = "Nav Bar Icon & Pill Bounce",
                            summary = "Apply Bounce Multiplier to the bottom navigation pill & active icons",
                            checked = config.navBarIconBounce,
                            enabled = config.enabled,
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) { checked ->
                            updateConfig(config.copy(navBarIconBounce = checked))
                        }
                    }
                }
            }
        }

        // Fullscreen live Splash Preview overlay when tapping "Preview"
        AnimatedVisibility(
            visible = previewingSplash,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val iconRes = remember(activity?.appIconKeyState?.value) {
                AppIconPreset.fromKey(activity?.appIconKeyState?.value).vectorDrawable
            }
            GombokSplashOverlay(
                iconRes = iconRes,
                animConfig = config,
                onFinished = { previewingSplash = false }
            )
        }
    }
}

@Composable
fun GombokSplashOverlay(
    iconRes: Int,
    animConfig: AnimationConfig,
    onFinished: () -> Unit
) {
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        started = true
        val totalDuration = animConfig.scaledDuration(1250).toLong()
        delay(totalDuration)
        onFinished()
    }

    val spinAngle by animateFloatAsState(
        targetValue = if (started) {
            when (animConfig.splashStyle) {
                0 -> 360f * 6f
                1 -> 360f * 2f
                else -> 0f
            }
        } else 0f,
        animationSpec = tween(
            durationMillis = animConfig.scaledDuration(1150),
            easing = FastOutSlowInEasing
        ),
        label = "splashSpin"
    )

    val flipY by animateFloatAsState(
        targetValue = if (started && animConfig.splashStyle == 3) 720f else 0f,
        animationSpec = tween(
            durationMillis = animConfig.scaledDuration(1100),
            easing = FastOutSlowInEasing
        ),
        label = "splashFlip"
    )

    val popScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.25f,
        animationSpec = animConfig.springSpec(stiffnessScale = 0.65f),
        label = "splashPop"
    )

    val ringProgress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = animConfig.scaledDuration(1100),
            easing = FastOutSlowInEasing
        ),
        label = "splashRing"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        if (animConfig.splashStyle == 1 || animConfig.splashStyle == 2) {
            // Expanding Material You pulse rings
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        val s = 1f + ringProgress * 1.9f
                        scaleX = s
                        scaleY = s
                        alpha = (1f - ringProgress).coerceIn(0f, 0.5f)
                    }
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            if (animConfig.splashStyle == 2) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            val s = 0.7f + ringProgress * 1.25f
                            scaleX = s
                            scaleY = s
                            alpha = (1f - ringProgress * 0.85f).coerceIn(0f, 0.35f)
                        }
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
                )
            }
        }

        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = if (animConfig.splashStyle == 0) 1f else popScale
                    scaleY = if (animConfig.splashStyle == 0) 1f else popScale
                    rotationZ = spinAngle
                    rotationY = flipY
                    cameraDistance = 12f * density
                }
        )
    }
}
