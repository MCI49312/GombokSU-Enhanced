package mci.github.gomboksu.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.viewModels
import androidx.navigation.NavBackStackEntry
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.lifecycleScope
import kotlin.math.abs
import kotlinx.coroutines.launch
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ExecuteModuleActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import mci.github.gomboksu.Natives
import mci.github.gomboksu.R
import mci.github.gomboksu.ksuApp
import mci.github.gomboksu.ui.screen.BottomBarDestination
import mci.github.gomboksu.ui.screen.FlashIt
import mci.github.gomboksu.ui.theme.KernelSUTheme
import mci.github.gomboksu.ui.util.*
import mci.github.gomboksu.ui.viewmodel.ModuleViewModel
import mci.github.gomboksu.ui.viewmodel.SuperUserViewModel

data class ScrollState(
    val isScrollingDown: MutableState<Boolean>,
    val scrollOffset: MutableState<Float>,
    val previousScrollOffset: MutableState<Float>,
)

val LocalScrollState = compositionLocalOf<ScrollState?> { null }
val LocalNavBarEnabled = compositionLocalOf<State<Boolean>?> { null }

@Composable
fun rememberScrollConnection(
    isScrollingDown: MutableState<Boolean>,
    scrollOffset: MutableState<Float>,
    previousScrollOffset: MutableState<Float>,
    threshold: Float = 50f
): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                
                // Update scroll offset
                val newOffset = scrollOffset.value + delta
                scrollOffset.value = newOffset
                
                // Calculate the scroll delta from previous offset
                val scrollDelta = previousScrollOffset.value - newOffset
                
                // Only update direction if scroll delta exceeds threshold
                if (abs(scrollDelta) > threshold) {
                    isScrollingDown.value = scrollDelta > 0
                    previousScrollOffset.value = newOffset
                }
                
                return Offset.Zero
            }
            
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Reset offset tracking after fling
                previousScrollOffset.value = scrollOffset.value
                return super.onPostFling(consumed, available)
            }
        }
    }
}

fun Modifier.horizontalSwipeNavigator(
    currentRoute: String?,
    destinations: List<BottomBarDestination>,
    isRtl: Boolean,
    onNavigate: (Int) -> Unit
): Modifier = pointerInput(currentRoute, isRtl) {

    var totalDrag = 0f

    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onHorizontalDrag = { change, dragAmount ->
            change.consume()
            totalDrag += dragAmount
        },
        onDragEnd = {
            val threshold = 150f

            if (kotlin.math.abs(totalDrag) > threshold) {
                val currentIndex = destinations.indexOfFirst {
                    it.direction.route == currentRoute
                }

                if (currentIndex == -1) return@detectHorizontalDragGestures

                if (if (isRtl) totalDrag > 0 else totalDrag < 0) {  
                    val next = (currentIndex + 1)
                        .coerceAtMost(destinations.lastIndex)
                    if (next != currentIndex) onNavigate(next)
                } else {
                    val prev = (currentIndex - 1)
                        .coerceAtLeast(0)
                    if (prev != currentIndex) onNavigate(prev)
                }
            }
        }
    )
}

fun Modifier.trackScroll(
    isScrollingDown: MutableState<Boolean>,
    scrollOffset: MutableState<Float>,
    previousScrollOffset: MutableState<Float>,
    threshold: Float = 50f
): Modifier {
    val scrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            
            // Update scroll offset
            val newOffset = scrollOffset.value + delta
            scrollOffset.value = newOffset
            
            // Calculate the scroll delta from previous offset
            val scrollDelta = previousScrollOffset.value - newOffset
            
            // Only update direction if scroll delta exceeds threshold
            if (abs(scrollDelta) > threshold) {
                isScrollingDown.value = scrollDelta > 0
                previousScrollOffset.value = newOffset
            }
            
            return Offset.Zero
        }
        
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            // Reset offset tracking after fling
            previousScrollOffset.value = scrollOffset.value
            return super.onPostFling(consumed, available)
        }
    }
    
    return this.nestedScroll(scrollConnection)
}

class MainActivity : FragmentActivity() {

    private val appLockState = mutableStateOf(false)

    private var pendingIntent: Intent? = null
    var zipUri by mutableStateOf<ArrayList<Uri>?>(null)
    enum class NavigateLocation { SUPERUSER, MODULES, SETTINGS }
    var navigateLoc by mutableStateOf<NavigateLocation?>(null)
    var moduleActionId by mutableStateOf<String?>(null)
    var amoledModeState = mutableStateOf(false)
    var themeModeState = mutableStateOf(0)
    var dynamicColorState = mutableStateOf(false)
    var accentPresetState = mutableStateOf("gombok_blue")
    var customAccentArgbState = mutableStateOf(0xFF3B82F6.toInt())
    var cardRadiusDpState = mutableStateOf(16)
    var fontStyleState = mutableStateOf("default")
    var appIconKeyState = mutableStateOf("gomboksu")
    var navBarEnabled = mutableStateOf(true)
    var navBarAutoHideState = mutableStateOf(true)
    var navBarStyleState = mutableStateOf(0)
    var navBarShowLabelsState = mutableStateOf(false)
    var swipeNavigationState = mutableStateOf(true)
    var hapticFeedbackState = mutableStateOf(true)
    var animationConfigState = mutableStateOf(AnimationConfig())
    private val handler = Handler(Looper.getMainLooper())

    val moduleViewModel: ModuleViewModel by viewModels()
    val superUserViewModel: SuperUserViewModel by viewModels()

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LocaleHelper.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            if (superUserViewModel.appList.isEmpty()) {
                superUserViewModel.fetchAppList()
            }
            if (moduleViewModel.moduleList.isEmpty()) {
                moduleViewModel.fetchModuleList()
            }
        }

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        try {
            val prefsInit = getSharedPreferences("settings", MODE_PRIVATE)
            amoledModeState.value = prefsInit.getBoolean("enable_amoled", false)
            themeModeState.value = prefsInit.getInt("theme_mode", 0)
            dynamicColorState.value = prefsInit.getBoolean("enable_dynamic_color", false)
            accentPresetState.value = prefsInit.getString("accent_preset", "gombok_blue") ?: "gombok_blue"
            customAccentArgbState.value = prefsInit.getInt("custom_accent_argb", 0xFF3B82F6.toInt())
            cardRadiusDpState.value = prefsInit.getInt("card_radius_dp", 16)
            fontStyleState.value = prefsInit.getString("font_style", "default") ?: "default"
            appIconKeyState.value = prefsInit.getString("app_icon_key", "gomboksu") ?: "gomboksu"
            navBarEnabled.value = prefsInit.getBoolean("enable_navbar", true)
            navBarAutoHideState.value = prefsInit.getBoolean("navbar_auto_hide", true)
            navBarStyleState.value = prefsInit.getInt("navbar_style", 0)
            navBarShowLabelsState.value = prefsInit.getBoolean("navbar_show_labels", false)
            swipeNavigationState.value = prefsInit.getBoolean("enable_swipe_navigation", true)
            hapticFeedbackState.value = prefsInit.getBoolean("enable_haptic_feedback", true)
            animationConfigState.value = AnimationConfig.fromPrefs(prefsInit)
        } catch (_: Exception) {}

        val isManager = Natives.isManager
        if (isManager) install()

        if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            intent.extras?.clear()
            intent = null
        }

        val prefsInit = getSharedPreferences("settings", MODE_PRIVATE)
        val enableStartupAnimation = prefsInit.getBoolean("enable_startup_animation", true)
        val requireBiometric = prefsInit.getBoolean("enable_biometric_lock", false)

        if (savedInstanceState != null) {
            appLockState.value = savedInstanceState.getBoolean("appLockState", requireBiometric)
        } else {
            appLockState.value = requireBiometric
        }

        if(intent != null)
            handleIntent(intent)

        setContent { Box(modifier = Modifier.fillMaxSize()) {
            KernelSUTheme(
                themeMode = themeModeState.value,
                dynamicColor = dynamicColorState.value,
                amoledMode = amoledModeState.value,
                accentPresetKey = accentPresetState.value,
                customAccentArgb = customAccentArgbState.value,
                cardRadiusDp = cardRadiusDpState.value,
                fontStyleKey = fontStyleState.value
            ) {
                var showSplash by remember { mutableStateOf(enableStartupAnimation) }
                val animConfig = animationConfigState.value

                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(animConfig.scaledDuration(220))) togetherWith
                            fadeOut(animationSpec = tween(animConfig.scaledDuration(220)))
                    },
                    label = "appSplashTransition"
                ) { isSplashVisible ->
                    if (isSplashVisible && !appLockState.value) {
                        val splashIconRes = remember(appIconKeyState.value) {
                            AppIconPreset.fromKey(appIconKeyState.value).vectorDrawable
                        }
                        mci.github.gomboksu.ui.screen.GombokSplashOverlay(
                            iconRes = splashIconRes,
                            animConfig = animConfig,
                            onFinished = { showSplash = false }
                        )
                    } else if (isSplashVisible) {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    } else {
                        val navController = rememberNavController()
                        val snackBarHostState = remember { SnackbarHostState() }
                        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
                        val bottomBarRoutes = remember {
                            BottomBarDestination.entries.map { it.direction.route }.toSet()
                        }
                        val navigator = navController.rememberDestinationsNavigator()

                        val isManager = Natives.isManager
                        val fullFeatured = Natives.isFullFeatured()

                        val currentBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = currentBackStackEntry?.destination?.route

                        val homeDestination = BottomBarDestination.entries.firstOrNull()
                        val startRoute = homeDestination?.direction?.route

                        if (homeDestination != null && startRoute != null) {
                            BackHandler(enabled = currentRoute != startRoute && currentRoute in bottomBarRoutes) {
                                navigator.navigate(homeDestination.direction) {
                                    popUpTo(NavGraphs.root) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }

                        // Track the last bottom bar destination index for directional animations
                        var lastBottomBarIndex by remember { mutableStateOf(0) }
                        var isBottomBarNavigation by remember { mutableStateOf(false) }

                        // Scroll state for bottom bar visibility
                        val isScrollingDown = remember { mutableStateOf(false) }
                        val scrollOffset = remember { mutableStateOf(0f) }
                        val previousScrollOffset = remember { mutableStateOf(0f) }

                        // Remember the last valid navbar selection (persists across navbar hide/show)
                        val lastValidNavbarSelection = remember { mutableStateOf(0) }

                        LaunchedEffect(currentDestination?.route) {
                            isScrollingDown.value = false
                            scrollOffset.value = 0f
                            previousScrollOffset.value = 0f
                        }

                        LaunchedEffect(navBarEnabled.value, navBarStyleState.value, navBarShowLabelsState.value, navBarAutoHideState.value) {
                            isScrollingDown.value = false
                            scrollOffset.value = 0f
                            previousScrollOffset.value = 0f
                        }

                        LaunchedEffect(zipUri, navigateLoc, moduleActionId) {
                            if (moduleActionId != null) {
                                navigator.navigate(ExecuteModuleActionScreenDestination(moduleActionId!!))
                                moduleActionId = null
                            }

                            if (!zipUri.isNullOrEmpty()) {
                                val uris = zipUri!!
                                val component = intent?.component?.className
                                val flashIt = when {
                                    // explicit legacy entry point
                                    component?.endsWith("FlashAnyKernel") == true -> FlashIt.FlashAnyKernel(uris.first())
                                    // auto-detect: single AnyKernel3 zip flashes as kernel, anything else as module(s)
                                    uris.size == 1 && ZipUtils.isAnyKernel3Zip(applicationContext, uris.first()) ->
                                        FlashIt.FlashAnyKernel(uris.first())
                                    else -> FlashIt.FlashModules(uris)
                                }

                                navigator.navigate(
                                    FlashScreenDestination(flashIt = flashIt)
                                )
                                zipUri = null
                            }

                            if (zipUri.isNullOrEmpty() && navigateLoc != null) {
                                when (navigateLoc) {
                                    NavigateLocation.SUPERUSER -> {
                                        val target = if (fullFeatured) SuperUserScreenDestination else HomeScreenDestination
                                        navigator.navigate(target) {
                                            popUpTo(NavGraphs.root.startRoute) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    NavigateLocation.MODULES -> {
                                        val target = if (fullFeatured) ModuleScreenDestination else HomeScreenDestination
                                        navigator.navigate(target) {
                                            popUpTo(NavGraphs.root.startRoute) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    NavigateLocation.SETTINGS -> navigator.navigate(SettingScreenDestination) {
                                        popUpTo(NavGraphs.root.startRoute) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    else -> { /* no-op for exhaustiveness */ }
                                }
                                navigateLoc = null
                            }
                        }

                        val showBottomBar = when (currentDestination?.route) {
                            FlashScreenDestination.route -> false // Hide for FlashScreenDestination
                            ExecuteModuleActionScreenDestination.route -> false // Hide for ExecuteModuleActionScreen
                            else -> (!isScrollingDown.value || !navBarAutoHideState.value) && navBarEnabled.value
                        }

                        Scaffold(
                            contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) { innerPadding ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                CompositionLocalProvider(
                                    LocalSnackbarHost provides snackBarHostState,
                                    LocalScrollState provides ScrollState(
                                        isScrollingDown = isScrollingDown,
                                        scrollOffset = scrollOffset,
                                        previousScrollOffset = previousScrollOffset,
                                    ),
                                    LocalNavBarEnabled provides navBarEnabled,
                                    LocalAnimationConfig provides animationConfigState.value
                                ) {
                                    val visibleDestinations = remember(fullFeatured) {
                                        BottomBarDestination.entries.filter { fullFeatured || !it.rootRequired }
                                    }

                                    fun navigateToIndex(index: Int) {
                                        val destination = visibleDestinations.getOrNull(index) ?: return
                                        if (destination.direction.route == currentRoute) return

                                        navigator.navigate(destination.direction) {
                                            popUpTo(NavGraphs.root.startRoute) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }

                                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                                    val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
                                    val animationDirection = if (isRtl) -1 else 1

                                    val hostModifier = Modifier
                                        .padding(innerPadding)
                                        .fillMaxSize()
                                        .let { mod ->
                                            if (swipeNavigationState.value) {
                                                mod.horizontalSwipeNavigator(
                                                    currentRoute = currentRoute,
                                                    destinations = visibleDestinations,
                                                    isRtl = isRtl,
                                                    onNavigate = {
                                                        if (hapticFeedbackState.value) {
                                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.GestureEnd)
                                                        }
                                                        navigateToIndex(it)
                                                    }
                                                )
                                            } else mod
                                        }

                                    DestinationsNavHost(
                                        modifier = hostModifier,
                                        navGraph = NavGraphs.root,
                                        navController = navController,
                                        defaultTransitions = object : NavHostAnimatedDestinationStyle() {
                                            override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                                                val cfg = animationConfigState.value
                                                val dur = cfg.scaledDuration(300)
                                                val targetRoute = targetState.destination.route
                                                val initialRoute = initialState.destination.route
                                                val targetIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == targetRoute }
                                                val initialIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == initialRoute }
                                                val offsetSign = when {
                                                    targetIndex != -1 && initialIndex != -1 -> (if (targetIndex > initialIndex) 1 else -1) * animationDirection
                                                    targetRoute in bottomBarRoutes && initialRoute !in bottomBarRoutes -> -1 * animationDirection
                                                    else -> 1 * animationDirection
                                                }
                                                if (!cfg.enabled) {
                                                    EnterTransition.None
                                                } else when (cfg.pageTransitionStyle) {
                                                    1 -> scaleIn(initialScale = 0.90f, animationSpec = cfg.springSpec(0.9f)) +
                                                        slideInHorizontally(initialOffsetX = { (it * 0.25f * offsetSign).toInt() }, animationSpec = cfg.springSpec(0.95f)) +
                                                        fadeIn(tween(dur))
                                                    2 -> slideInHorizontally(initialOffsetX = { it * offsetSign }, animationSpec = cfg.springSpec(0.9f)) +
                                                        fadeIn(tween(dur))
                                                    3 -> slideInVertically(initialOffsetY = { (it * 0.16f).toInt() }, animationSpec = cfg.springSpec(0.95f)) +
                                                        fadeIn(tween(dur))
                                                    4 -> scaleIn(initialScale = 0.80f, animationSpec = cfg.springSpec(0.85f)) +
                                                        fadeIn(tween(dur))
                                                    else -> slideInHorizontally(initialOffsetX = { it * offsetSign }, animationSpec = tween(dur))
                                                }
                                            }

                                            override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                                                val cfg = animationConfigState.value
                                                val dur = cfg.scaledDuration(260)
                                                val targetRoute = targetState.destination.route
                                                val initialRoute = initialState.destination.route
                                                val targetIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == targetRoute }
                                                val initialIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == initialRoute }
                                                val offsetSign = when {
                                                    targetIndex != -1 && initialIndex != -1 -> (if (targetIndex > initialIndex) -1 else 1) * animationDirection
                                                    else -> -1 * animationDirection
                                                }
                                                if (!cfg.enabled) {
                                                    ExitTransition.None
                                                } else when (cfg.pageTransitionStyle) {
                                                    1 -> scaleOut(targetScale = 0.94f, animationSpec = tween(dur)) + fadeOut(tween(dur))
                                                    2 -> scaleOut(targetScale = 0.88f, animationSpec = tween(dur)) + fadeOut(tween(dur))
                                                    3 -> slideOutVertically(targetOffsetY = { -(it * 0.08f).toInt() }, animationSpec = tween(dur)) + fadeOut(tween(dur))
                                                    4 -> scaleOut(targetScale = 0.90f, animationSpec = tween(dur)) + fadeOut(tween(dur))
                                                    else -> slideOutHorizontally(targetOffsetX = { it * offsetSign }, animationSpec = tween(dur))
                                                }
                                            }

                                            override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                                                val cfg = animationConfigState.value
                                                val dur = cfg.scaledDuration(280)
                                                val targetRoute = targetState.destination.route
                                                val initialRoute = initialState.destination.route
                                                val targetIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == targetRoute }
                                                val initialIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == initialRoute }
                                                val offsetSign = when {
                                                    targetIndex != -1 && initialIndex != -1 -> (if (targetIndex > initialIndex) 1 else -1) * animationDirection
                                                    else -> -1 * animationDirection
                                                }
                                                if (!cfg.enabled) {
                                                    EnterTransition.None
                                                } else when (cfg.pageTransitionStyle) {
                                                    1 -> scaleIn(initialScale = 0.94f, animationSpec = cfg.springSpec(0.95f)) + fadeIn(tween(dur))
                                                    2 -> scaleIn(initialScale = 0.88f, animationSpec = cfg.springSpec(0.95f)) + fadeIn(tween(dur))
                                                    3 -> slideInVertically(initialOffsetY = { -(it * 0.12f).toInt() }, animationSpec = cfg.springSpec(0.95f)) + fadeIn(tween(dur))
                                                    4 -> scaleIn(initialScale = 0.86f, animationSpec = cfg.springSpec(0.9f)) + fadeIn(tween(dur))
                                                    else -> slideInHorizontally(initialOffsetX = { it * offsetSign }, animationSpec = tween(dur))
                                                }
                                            }

                                            override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                                                val cfg = animationConfigState.value
                                                val dur = cfg.scaledDuration(260)
                                                val targetRoute = targetState.destination.route
                                                val initialRoute = initialState.destination.route
                                                val targetIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == targetRoute }
                                                val initialIndex = BottomBarDestination.entries.indexOfFirst { it.direction.route == initialRoute }
                                                val offsetSign = when {
                                                    targetIndex != -1 && initialIndex != -1 -> (if (targetIndex > initialIndex) -1 else 1) * animationDirection
                                                    initialRoute !in bottomBarRoutes -> 1 * animationDirection
                                                    else -> -1 * animationDirection
                                                }
                                                if (!cfg.enabled) {
                                                    ExitTransition.None
                                                } else when (cfg.pageTransitionStyle) {
                                                    1 -> scaleOut(targetScale = 0.90f, animationSpec = tween(dur)) +
                                                        slideOutHorizontally(targetOffsetX = { (it * 0.25f * offsetSign).toInt() }, animationSpec = tween(dur)) +
                                                        fadeOut(tween(dur))
                                                    2 -> slideOutHorizontally(targetOffsetX = { it * offsetSign }, animationSpec = tween(dur)) + fadeOut(tween(dur))
                                                    3 -> slideOutVertically(targetOffsetY = { (it * 0.16f).toInt() }, animationSpec = tween(dur)) + fadeOut(tween(dur))
                                                    4 -> scaleOut(targetScale = 0.82f, animationSpec = tween(dur)) + fadeOut(tween(dur))
                                                    else -> slideOutHorizontally(targetOffsetX = { it * offsetSign }, animationSpec = tween(dur))
                                                }
                                            }
                                        }
                                    )
                                }

                                // Floating Bottom Bar as overlay
                                AnimatedVisibility(
                                    visible = showBottomBar,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = animationConfigState.value.springSpec(1.0f)) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                ) {
                                    BottomBar(
                                        navController = navController,
                                        lastValidSelection = lastValidNavbarSelection,
                                        navBarStyle = navBarStyleState.value,
                                        showLabels = navBarShowLabelsState.value,
                                        animConfig = animationConfigState.value
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AppLockOverlay(amoledModeState.value)

        } } // Box & setContent
    } // onCreate

    @Composable
    private fun AppLockOverlay(amoledMode: Boolean) {
        if (appLockState.value) {
            KernelSUTheme(
                themeMode = themeModeState.value,
                dynamicColor = dynamicColorState.value,
                amoledMode = amoledMode,
                accentPresetKey = accentPresetState.value,
                customAccentArgb = customAccentArgbState.value,
                cardRadiusDp = cardRadiusDpState.value,
                fontStyleKey = fontStyleState.value
            ) {
                androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    var isPromptShowing by remember { mutableStateOf(false) }
                    DisposableEffect(lifecycleOwner) {
                        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                                val timeout = prefs.getLong("app_lock_timeout", 60000L)
                                if (!mci.github.gomboksu.ui.util.AppLockManager.shouldPrompt(timeout)) {
                                    appLockState.value = false
                                    pendingIntent?.let {
                                        handleIntent(it)
                                        pendingIntent = null
                                    }
                                    return@LifecycleEventObserver
                                }

                                if (!isPromptShowing) {
                                    isPromptShowing = true
                                    mci.github.gomboksu.ui.util.BiometricAuthenticator(this@MainActivity)
                                        .authenticate(
                                            title = getString(mci.github.gomboksu.R.string.biometric_prompt_subtitle),
                                            subtitle = null,
                                            onSuccess = {
                                                isPromptShowing = false
                                                appLockState.value = false
                                                mci.github.gomboksu.ui.util.AppLockManager.unlock()
                                                pendingIntent?.let {
                                                    handleIntent(it)
                                                    pendingIntent = null
                                                }
                                            },
                                            onError = {
                                                isPromptShowing = false
                                                Toast.makeText(this@MainActivity, getString(R.string.auth_failed, it), Toast.LENGTH_SHORT).show()
                                                finish()
                                            }
                                        )
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }
                }
            }
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putBoolean("enable_amoled", enabled).apply()
        } catch (_: Exception) {}
        amoledModeState.value = enabled
    }

    fun setThemeMode(mode: Int) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putInt("theme_mode", mode).apply()
        } catch (_: Exception) {}
        themeModeState.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putBoolean("enable_dynamic_color", enabled).apply()
        } catch (_: Exception) {}
        dynamicColorState.value = enabled
    }

    fun setAccentPreset(key: String) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putString("accent_preset", key).apply()
        } catch (_: Exception) {}
        accentPresetState.value = key
    }

    fun setCustomAccentArgb(argb: Int) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putInt("custom_accent_argb", argb).apply()
        } catch (_: Exception) {}
        customAccentArgbState.value = argb
    }

    fun setCardRadiusDp(radiusDp: Int) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putInt("card_radius_dp", radiusDp).apply()
        } catch (_: Exception) {}
        cardRadiusDpState.value = radiusDp
    }

    fun setFontStyle(styleKey: String) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putString("font_style", styleKey).apply()
        } catch (_: Exception) {}
        fontStyleState.value = styleKey
    }

    fun setAppIconKey(iconKey: String) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putString("app_icon_key", iconKey).apply()
        } catch (_: Exception) {}
        appIconKeyState.value = iconKey
        AppIconPreset.applyAppIcon(this, iconKey)
    }

    fun setNavBarEnabled(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putBoolean("enable_navbar", enabled).apply()
        } catch (_: Exception) {}
        navBarEnabled.value = enabled
    }

    fun setNavBarAutoHide(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putBoolean("navbar_auto_hide", enabled).apply()
        } catch (_: Exception) {}
        navBarAutoHideState.value = enabled
    }

    fun setNavBarStyle(style: Int) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putInt("navbar_style", style).apply()
        } catch (_: Exception) {}
        navBarStyleState.value = style
    }

    fun setNavBarShowLabels(show: Boolean) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putBoolean("navbar_show_labels", show).apply()
        } catch (_: Exception) {}
        navBarShowLabelsState.value = show
    }

    fun setSwipeNavigation(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putBoolean("enable_swipe_navigation", enabled).apply()
        } catch (_: Exception) {}
        swipeNavigationState.value = enabled
    }

    fun setHapticFeedback(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit().putBoolean("enable_haptic_feedback", enabled).apply()
        } catch (_: Exception) {}
        hapticFeedbackState.value = enabled
    }

    fun updateAnimationConfig(config: AnimationConfig) {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("anim_enabled", config.enabled)
                .putFloat("anim_bounce_multiplier", config.bounceMultiplier)
                .putFloat("anim_speed_multiplier", config.speedMultiplier)
                .putInt("anim_page_transition", config.pageTransitionStyle)
                .putInt("anim_splash_style", config.splashStyle)
                .putBoolean("anim_card_press_bounce", config.cardPressBounce)
                .putBoolean("anim_staggered_entrance", config.staggeredEntrance)
                .putBoolean("anim_status_pulse_glow", config.statusPulseGlow)
                .putBoolean("anim_navbar_icon_bounce", config.navBarIconBounce)
                .apply()
        } catch (_: Exception) {}
        animationConfigState.value = config
    }

    override fun onStart() {
        super.onStart()
        mci.github.gomboksu.ui.util.AppLockManager.onActivityStart()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        setIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        mci.github.gomboksu.ui.util.AppLockManager.onActivityStop(this)
        val prefsInit = getSharedPreferences("settings", MODE_PRIVATE)
        if (prefsInit.getBoolean("enable_biometric_lock", false)) {
            appLockState.value = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("appLockState", appLockState.value)

    }

    private fun handleIntent(intent: Intent) {
        if (appLockState.value) {
            pendingIntent = intent
            return
        }
        val shortcutType = intent.getStringExtra("shortcut_type")
        if (shortcutType == "module_action") {
            moduleActionId = intent.getStringExtra("module_id")
        }

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                zipUri =
                    intent.data?.let { arrayListOf(it) }
                        ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableArrayListExtra("uris", Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableArrayListExtra("uris")
                        }
            }

            "ACTION_SETTINGS" -> navigateLoc = NavigateLocation.SETTINGS
            "ACTION_SUPERUSER" -> navigateLoc = NavigateLocation.SUPERUSER
            "ACTION_MODULES" -> navigateLoc = NavigateLocation.MODULES
            else -> { /* ignore other actions */ }
        }
    }
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    lastValidSelection: MutableState<Int>,
    navBarStyle: Int = 0, // 0 = Floating Pill, 1 = Classic Docked, 2 = Compact Pill
    showLabels: Boolean = false,
    animConfig: AnimationConfig = AnimationConfig()
) {
    val navigator = navController.rememberDestinationsNavigator()
    val isManager = Natives.isManager
    val fullFeatured = Natives.isFullFeatured()

    val visibleDestinations = remember(fullFeatured) {
        BottomBarDestination.entries.filter { fullFeatured || !it.rootRequired }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val isOnBackStack = visibleDestinations.map { destination ->
        navController.isRouteOnBackStackAsState(destination.direction).value
    }

    val selectedIndex = run {
        val exactMatch = visibleDestinations.indexOfFirst { it.direction.route == currentRoute }
        if (exactMatch != -1) exactMatch
        else isOnBackStack.indexOfLast { it }
    }

    if (selectedIndex != -1) lastValidSelection.value = selectedIndex
    val effectiveSelectedIndex = if (selectedIndex != -1) selectedIndex else lastValidSelection.value

    // Drag state
    var isDraggingPill by remember { mutableStateOf(false) }
    var dragTargetIndex by remember { mutableStateOf(effectiveSelectedIndex) }

    // During drag, animate toward dragTargetIndex; otherwise animate toward effectiveSelectedIndex
    val animatedSelectedIndex by animateFloatAsState(
        targetValue = (if (isDraggingPill) dragTargetIndex else effectiveSelectedIndex).toFloat(),
        animationSpec = if (isDraggingPill) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        } else if (animConfig.enabled && animConfig.navBarIconBounce) {
            animConfig.springSpec(stiffnessScale = 0.85f)
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        label = "selectedIndex"
    )

    fun navigateToIndex(index: Int) {
        val destination = visibleDestinations.getOrNull(index) ?: return
        if (destination.direction.route == currentRoute) return
        navigator.navigate(destination.direction) {
            popUpTo(NavGraphs.root.startRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (navBarStyle == 1) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp
        ) {
            visibleDestinations.forEachIndexed { index, destination ->
                val isSelected = index == effectiveSelectedIndex
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { navigateToIndex(index) },
                    icon = {
                        Icon(
                            if (isSelected) destination.iconSelected else destination.iconNotSelected,
                            stringResource(destination.label)
                        )
                    },
                    label = if (showLabels) {
                        { Text(stringResource(destination.label)) }
                    } else null,
                    alwaysShowLabel = showLabels
                )
            }
        }
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                )
        ) {
            val screenWidth = maxWidth
            val horizontalScreenPadding = when {
                screenWidth > 600.dp -> 32.dp
                screenWidth > 400.dp -> 24.dp
                else -> 16.dp
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalScreenPadding, vertical = if (navBarStyle == 2) 10.dp else 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.wrapContentWidth(),
                    shape = RoundedCornerShape(if (navBarStyle == 2) 20.dp else 24.dp),
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    val itemSize = if (navBarStyle == 2) 48.dp else if (showLabels) 64.dp else 56.dp
                    val barHeight = if (navBarStyle == 2) 60.dp else if (showLabels) 76.dp else 72.dp
                    val itemSpacing = 4.dp
                    val containerPadding = 7.dp

                    val navBarWidth = (itemSize * visibleDestinations.size) +
                            (itemSpacing * (visibleDestinations.size - 1)) +
                            (containerPadding * 2)

                    val density = LocalDensity.current
                    val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
                    val itemSizePx = with(density) { itemSize.toPx() }
                    val itemSpacingPx = with(density) { itemSpacing.toPx() }
                    val containerPaddingPx = with(density) { containerPadding.toPx() }

                    Box(
                        modifier = Modifier
                            .width(navBarWidth)
                            .height(barHeight)
                            .pointerInput(visibleDestinations, effectiveSelectedIndex, isRtl, navBarStyle, showLabels) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val extraTouchArea = with(density) { 20.dp.toPx() }

                                        val visualIndex = if (isRtl) visibleDestinations.lastIndex - effectiveSelectedIndex else effectiveSelectedIndex

                                        val pillLeft = containerPaddingPx +
                                                visualIndex * (itemSizePx + itemSpacingPx) - extraTouchArea

                                        val pillRight = pillLeft + itemSizePx + (extraTouchArea * 2)

                                        if (offset.x in pillLeft..pillRight) {
                                            isDraggingPill = true
                                            dragTargetIndex = effectiveSelectedIndex
                                        }
                                    },
                                    onDragEnd = {
                                        if (isDraggingPill) {
                                            navigateToIndex(dragTargetIndex)
                                            isDraggingPill = false
                                        }
                                    },
                                    onDragCancel = {
                                        isDraggingPill = false
                                    },
                                    onDrag = { change, _ ->
                                        if (isDraggingPill) {
                                            change.consume()
                                            // Map finger X to nearest icon index
                                            val visualIndex = ((change.position.x - containerPaddingPx) /
                                                    (itemSizePx + itemSpacingPx))
                                                .toInt()
                                                .coerceIn(0, visibleDestinations.lastIndex)
                                            dragTargetIndex = if (isRtl) visibleDestinations.lastIndex - visualIndex else visualIndex
                                        }
                                    }
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = containerPadding)
                        ) {
                            // Sliding pill indicator
                            if (visibleDestinations.isNotEmpty()) {
                                val indicatorOffset = (itemSizePx + itemSpacingPx) * animatedSelectedIndex

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(vertical = 8.dp)
                                        .offset {
                                            IntOffset(x = indicatorOffset.toInt(), y = 0)
                                        }
                                        .width(itemSize)
                                        // Subtle scale-up when dragging, like iOS
                                        .graphicsLayer {
                                            scaleX = if (isDraggingPill) 1.1f else 1f
                                            scaleY = if (isDraggingPill) 1.1f else 1f
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(itemSize)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    )
                                }
                            }
                            // Navigation items
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                visibleDestinations.forEachIndexed { index, destination ->
                                    val isSelected = index == (if (isDraggingPill) dragTargetIndex else effectiveSelectedIndex)

                                    Box(
                                        modifier = Modifier
                                            .size(itemSize)
                                            .clip(MaterialTheme.shapes.large)
                                            .clickable {
                                                if (destination.direction.route == currentRoute) return@clickable
                                                navigateToIndex(index)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val iconScale by animateFloatAsState(
                                            targetValue = if (isSelected && animConfig.enabled && animConfig.navBarIconBounce) 1.15f else 1.0f,
                                            animationSpec = if (animConfig.enabled && animConfig.navBarIconBounce) {
                                                animConfig.springSpec(stiffnessScale = 1.1f)
                                            } else {
                                                tween(120)
                                            },
                                            label = "navIconScale_$index"
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                            }
                                        ) {
                                            Icon(
                                                if (isSelected) destination.iconSelected else destination.iconNotSelected,
                                                stringResource(destination.label),
                                                tint = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            if (showLabels) {
                                                Text(
                                                    text = stringResource(destination.label),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
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
        }
    }
}
