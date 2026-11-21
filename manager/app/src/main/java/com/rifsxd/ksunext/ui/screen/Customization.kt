package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.LocalNavBarEnabled
import com.rifsxd.ksunext.ui.LocalScrollState
import com.rifsxd.ksunext.ui.MainActivity
import com.rifsxd.ksunext.ui.component.SwitchItem
import com.rifsxd.ksunext.ui.rememberScrollConnection
import com.rifsxd.ksunext.ui.theme.AccentPreset
import com.rifsxd.ksunext.ui.util.LocalSnackbarHost
import com.rifsxd.ksunext.ui.util.LocaleHelper
import com.rifsxd.ksunext.ui.util.refreshActivity
import kotlin.math.roundToInt

/**
 * GombokSU Enhanced Customization Studio
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CustomizationScreen(navigator: DestinationsNavigator) {
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
    val snackBarHost = LocalSnackbarHost.current

    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null

    val scrollState = LocalScrollState.current
    val navBarEnabled = LocalNavBarEnabled.current
    val isNavBarHidden = (scrollState?.isScrollingDown?.value ?: false) || (navBarEnabled?.value == false)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (isNavBarHidden) 0.dp else 112.dp

    val context = LocalContext.current
    val activity = context as? MainActivity
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var currentAppLocale by remember {
        mutableStateOf(LocaleHelper.getCurrentAppLocale(context))
    }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }
    var showHeaderTitleDialog by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        currentAppLocale = LocaleHelper.getCurrentAppLocale(context)
    }

    val languageOptions = remember(context) {
        listOf(
            LocaleHelper.SYSTEM_LANGUAGE_TAG to context.getString(R.string.system_default)
        ) + LocaleHelper.getSupportedLocales(context)
            .map { it.toLanguageTag() to it.getDisplayName(it) }
            .sortedBy { (_, displayName) -> displayName }
    }
    val currentLanguageTag = currentAppLocale?.toLanguageTag()
        ?: LocaleHelper.SYSTEM_LANGUAGE_TAG

    // State variables backed by prefs & MainActivity reactive states
    var themeMode by rememberSaveable { mutableIntStateOf(prefs.getInt("theme_mode", 0)) }
    var dynamicColor by rememberSaveable { mutableStateOf(prefs.getBoolean("enable_dynamic_color", false)) }
    var enableAmoled by rememberSaveable { mutableStateOf(prefs.getBoolean("enable_amoled", false)) }
    var accentPresetKey by rememberSaveable { mutableStateOf(prefs.getString("accent_preset", "gombok_blue") ?: "gombok_blue") }
    var customAccentArgb by rememberSaveable { mutableIntStateOf(prefs.getInt("custom_accent_argb", 0xFF3B82F6.toInt())) }
    var cardRadiusDp by rememberSaveable { mutableIntStateOf(prefs.getInt("card_radius_dp", 16)) }
    var fontStyleKey by rememberSaveable { mutableStateOf(prefs.getString("font_style", "default") ?: "default") }

    var enableNavBar by rememberSaveable { mutableStateOf(prefs.getBoolean("enable_navbar", true)) }
    var navBarAutoHide by rememberSaveable { mutableStateOf(prefs.getBoolean("navbar_auto_hide", true)) }
    var navBarStyle by rememberSaveable { mutableIntStateOf(prefs.getInt("navbar_style", 0)) }
    var navBarShowLabels by rememberSaveable { mutableStateOf(prefs.getBoolean("navbar_show_labels", false)) }
    var swipeNavigation by rememberSaveable { mutableStateOf(prefs.getBoolean("enable_swipe_navigation", true)) }
    var hapticFeedback by rememberSaveable { mutableStateOf(prefs.getBoolean("enable_haptic_feedback", true)) }
    var enableStartupAnimation by rememberSaveable { mutableStateOf(prefs.getBoolean("enable_startup_animation", true)) }

    var useBanner by rememberSaveable { mutableStateOf(prefs.getBoolean("use_banner", true)) }
    var autoExpandInfo by rememberSaveable { mutableStateOf(prefs.getBoolean("home_auto_expand_info", false)) }
    var showContributors by rememberSaveable { mutableStateOf(prefs.getBoolean("home_show_contributors", true)) }
    var showIssueReport by rememberSaveable { mutableStateOf(prefs.getBoolean("home_show_issue_report", true)) }
    var customHeaderTitle by rememberSaveable { mutableStateOf(prefs.getString("custom_header_title", "GombokSU Enhanced") ?: "GombokSU Enhanced") }
    var customHeaderSubtitle by rememberSaveable { mutableStateOf(prefs.getString("custom_header_subtitle", "") ?: "") }

    val effectiveDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    Scaffold(
        topBar = {
            TopBar(
                onBack = dropUnlessResumed {
                    navigator.popBackStack()
                },
                onReset = {
                    prefs.edit {
                        putInt("theme_mode", 0)
                        putBoolean("enable_dynamic_color", false)
                        putBoolean("enable_amoled", false)
                        putString("accent_preset", "gombok_blue")
                        putInt("custom_accent_argb", 0xFF3B82F6.toInt())
                        putInt("card_radius_dp", 16)
                        putString("font_style", "default")
                        putBoolean("enable_navbar", true)
                        putBoolean("navbar_auto_hide", true)
                        putInt("navbar_style", 0)
                        putBoolean("navbar_show_labels", false)
                        putBoolean("enable_swipe_navigation", true)
                        putBoolean("enable_haptic_feedback", true)
                        putBoolean("enable_startup_animation", true)
                        putBoolean("use_banner", true)
                        putBoolean("home_auto_expand_info", false)
                        putBoolean("home_show_contributors", true)
                        putBoolean("home_show_issue_report", true)
                        putString("custom_header_title", "GombokSU Enhanced")
                        putString("custom_header_subtitle", "")
                    }
                    themeMode = 0
                    dynamicColor = false
                    enableAmoled = false
                    accentPresetKey = "gombok_blue"
                    customAccentArgb = 0xFF3B82F6.toInt()
                    cardRadiusDp = 16
                    fontStyleKey = "default"
                    enableNavBar = true
                    navBarAutoHide = true
                    navBarStyle = 0
                    navBarShowLabels = false
                    swipeNavigation = true
                    hapticFeedback = true
                    enableStartupAnimation = true
                    useBanner = true
                    autoExpandInfo = false
                    showContributors = true
                    showIssueReport = true
                    customHeaderTitle = "GombokSU Enhanced"
                    customHeaderSubtitle = ""

                    activity?.setThemeMode(0)
                    activity?.setDynamicColor(false)
                    activity?.setAmoledMode(false)
                    activity?.setAccentPreset("gombok_blue")
                    activity?.setCustomAccentArgb(0xFF3B82F6.toInt())
                    activity?.setCardRadiusDp(16)
                    activity?.setFontStyle("default")
                    activity?.setNavBarEnabled(true)
                    activity?.setNavBarAutoHide(true)
                    activity?.setNavBarStyle(0)
                    activity?.setNavBarShowLabels(false)
                    activity?.setSwipeNavigation(true)
                    activity?.setHapticFeedback(true)

                    Toast.makeText(context, R.string.customization_reset_done, Toast.LENGTH_SHORT).show()
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost, modifier = Modifier.padding(bottom = navBarPadding)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Theme Preview Banner
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customHeaderTitle.ifBlank { "GombokSU Enhanced" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        val activeAccentName = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            stringResource(R.string.dynamic_monet_wallpaper)
                        } else {
                            AccentPreset.fromKey(accentPresetKey).displayName
                        }
                        Text(
                            text = stringResource(R.string.active_theme_format, activeAccentName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    FilledTonalButton(
                        onClick = { showCustomColorDialog = true }
                    ) {
                        Icon(Icons.Filled.Colorize, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.rgb_picker))
                    }
                }
            }

            // SECTION 1: ACCENT COLOR & THEME ENGINE
            SectionCard(
                title = stringResource(R.string.section_theme_accent),
                icon = Icons.Filled.Palette
            ) {
                // Theme Mode Segmented Selector (System / Light / Dark)
                Text(
                    text = stringResource(R.string.theme_mode_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                val themeModes = listOf(
                    0 to stringResource(R.string.theme_mode_system),
                    1 to stringResource(R.string.theme_mode_light),
                    2 to stringResource(R.string.theme_mode_dark)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themeModes.forEach { (mode, label) ->
                        val selected = themeMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = {
                                themeMode = mode
                                activity?.setThemeMode(mode)
                            },
                            label = { Text(label) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchItem(
                        icon = Icons.Filled.Wallpaper,
                        title = stringResource(R.string.settings_dynamic_color),
                        summary = stringResource(R.string.settings_dynamic_color_summary),
                        checked = dynamicColor,
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) { checked ->
                        dynamicColor = checked
                        activity?.setDynamicColor(checked)
                    }
                }

                if (effectiveDark) {
                    SwitchItem(
                        icon = Icons.Filled.Contrast,
                        title = stringResource(id = R.string.settings_amoled_mode),
                        summary = stringResource(id = R.string.settings_amoled_mode_summary),
                        checked = enableAmoled,
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) { checked ->
                        activity?.setAmoledMode(checked)
                        enableAmoled = checked
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                // Accent Color Swatches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.accent_color_presets),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (dynamicColor) stringResource(R.string.accent_disabled_by_monet)
                            else AccentPreset.fromKey(accentPresetKey).displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = {
                        dynamicColor = false
                        activity?.setDynamicColor(false)
                        showCustomColorDialog = true
                    }) {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.custom_hex_rgb))
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(AccentPreset.entries, key = { it.key }) { preset ->
                        val isSelected = !dynamicColor && accentPresetKey == preset.key
                        val swatchColor = if (preset == AccentPreset.CUSTOM) Color(customAccentArgb) else preset.primary
                        val checkTint = if (swatchColor.luminance() > 0.5f) Color.Black else Color.White

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(68.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    dynamicColor = false
                                    activity?.setDynamicColor(false)
                                    accentPresetKey = preset.key
                                    activity?.setAccentPreset(preset.key)
                                    if (preset == AccentPreset.CUSTOM) {
                                        showCustomColorDialog = true
                                    }
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = preset.displayName,
                                        tint = checkTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else if (preset == AccentPreset.CUSTOM) {
                                    Icon(
                                        imageVector = Icons.Filled.Colorize,
                                        contentDescription = preset.displayName,
                                        tint = checkTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = preset.displayName.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // SECTION 2: UI STYLE, SHAPES & TYPOGRAPHY
            SectionCard(
                title = stringResource(R.string.section_shapes_typography),
                icon = Icons.Filled.RoundedCorner
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.card_corner_radius),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${cardRadiusDp} dp",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = cardRadiusDp.toFloat(),
                        onValueChange = {
                            val newRadius = it.roundToInt()
                            cardRadiusDp = newRadius
                            activity?.setCardRadiusDp(newRadius)
                        },
                        valueRange = 4f..28f,
                        steps = 11
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                Text(
                    text = stringResource(R.string.font_family_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                val fontOptions = listOf(
                    "default" to "Default",
                    "sans" to "Sans Serif",
                    "monospace" to "Monospace",
                    "serif" to "Serif"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fontOptions.forEach { (key, label) ->
                        val selected = fontStyleKey == key
                        FilterChip(
                            selected = selected,
                            onClick = {
                                fontStyleKey = key
                                activity?.setFontStyle(key)
                            },
                            label = { Text(label, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                val language = stringResource(id = R.string.settings_language)
                val currentLanguageDisplay = currentAppLocale?.let { it.getDisplayName(it) }
                    ?: stringResource(R.string.system_default)

                ListItem(
                    leadingContent = { Icon(Icons.Filled.Translate, language) },
                    headlineContent = {
                        Text(
                            text = language,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = { Text(currentLanguageDisplay) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        showLanguageSheet = true
                    }
                )
            }

            // SECTION 3: NAVIGATION & INTERACTION
            SectionCard(
                title = stringResource(R.string.section_navigation_behavior),
                icon = Icons.Filled.Dock
            ) {
                SwitchItem(
                    icon = Icons.Filled.Dock,
                    title = stringResource(id = R.string.settings_navbar),
                    summary = stringResource(id = R.string.settings_navbar_summary),
                    checked = enableNavBar,
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) { checked ->
                    activity?.setNavBarEnabled(checked)
                    enableNavBar = checked
                }

                if (enableNavBar) {
                    Text(
                        text = stringResource(R.string.navbar_style_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    val navStyles = listOf(
                        0 to stringResource(R.string.navbar_style_floating),
                        1 to stringResource(R.string.navbar_style_classic),
                        2 to stringResource(R.string.navbar_style_compact)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        navStyles.forEach { (styleId, label) ->
                            val selected = navBarStyle == styleId
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    navBarStyle = styleId
                                    activity?.setNavBarStyle(styleId)
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    SwitchItem(
                        icon = Icons.Filled.Label,
                        title = stringResource(R.string.navbar_show_labels),
                        summary = stringResource(R.string.navbar_show_labels_summary),
                        checked = navBarShowLabels,
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) { checked ->
                        navBarShowLabels = checked
                        activity?.setNavBarShowLabels(checked)
                    }

                    SwitchItem(
                        icon = Icons.Filled.UnfoldLess,
                        title = stringResource(R.string.navbar_auto_hide),
                        summary = stringResource(R.string.navbar_auto_hide_summary),
                        checked = navBarAutoHide,
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) { checked ->
                        navBarAutoHide = checked
                        activity?.setNavBarAutoHide(checked)
                    }
                }

                SwitchItem(
                    icon = Icons.Filled.Swipe,
                    title = stringResource(R.string.settings_swipe_navigation),
                    summary = stringResource(R.string.settings_swipe_navigation_summary),
                    checked = swipeNavigation,
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) { checked ->
                    swipeNavigation = checked
                    activity?.setSwipeNavigation(checked)
                }

                SwitchItem(
                    icon = Icons.Filled.Vibration,
                    title = stringResource(R.string.settings_haptic_feedback),
                    summary = stringResource(R.string.settings_haptic_feedback_summary),
                    checked = hapticFeedback,
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) { checked ->
                    hapticFeedback = checked
                    activity?.setHapticFeedback(checked)
                }

                SwitchItem(
                    icon = Icons.Filled.Animation,
                    title = stringResource(id = R.string.settings_startup_animation),
                    summary = stringResource(id = R.string.settings_startup_animation_summary),
                    checked = enableStartupAnimation,
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) { checked ->
                    prefs.edit { putBoolean("enable_startup_animation", checked) }
                    enableStartupAnimation = checked
                }
            }

            // SECTION 4: HOME DASHBOARD & MODULES
            SectionCard(
                title = stringResource(R.string.section_home_dashboard),
                icon = Icons.Filled.DashboardCustomize
            ) {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Title, null) },
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.custom_header_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        val sub = if (customHeaderSubtitle.isNotBlank()) " • $customHeaderSubtitle" else ""
                        Text("${customHeaderTitle.ifBlank { "GombokSU Enhanced" }}$sub")
                    },
                    trailingContent = {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        showHeaderTitleDialog = true
                    }
                )

                if (ksuVersion != null) {
                    SwitchItem(
                        icon = Icons.Filled.ViewCarousel,
                        title = stringResource(id = R.string.settings_banner),
                        summary = stringResource(id = R.string.settings_banner_summary),
                        checked = useBanner,
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        prefs.edit { putBoolean("use_banner", it) }
                        useBanner = it
                    }
                }

                SwitchItem(
                    icon = Icons.Filled.ExpandCircleDown,
                    title = stringResource(R.string.home_auto_expand_info),
                    summary = stringResource(R.string.home_auto_expand_info_summary),
                    checked = autoExpandInfo,
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) { checked ->
                    prefs.edit { putBoolean("home_auto_expand_info", checked) }
                    autoExpandInfo = checked
                }

                SwitchItem(
                    icon = Icons.Filled.BugReport,
                    title = stringResource(R.string.home_show_issue_report),
                    summary = stringResource(R.string.home_show_issue_report_summary),
                    checked = showIssueReport,
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) { checked ->
                    prefs.edit { putBoolean("home_show_issue_report", checked) }
                    showIssueReport = checked
                }

                SwitchItem(
                    icon = Icons.Filled.Group,
                    title = stringResource(R.string.home_show_contributors),
                    summary = stringResource(R.string.home_show_contributors_summary),
                    checked = showContributors,
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) { checked ->
                    prefs.edit { putBoolean("home_show_contributors", checked) }
                    showContributors = checked
                }
            }
        }
    }

    if (showLanguageSheet) {
        LanguageBottomSheet(
            options = languageOptions,
            selectedLanguageTag = currentLanguageTag,
            onDismiss = { showLanguageSheet = false },
            onLanguageSelected = { languageTag ->
                showLanguageSheet = false
                if (languageTag != currentLanguageTag) {
                    LocaleHelper.setAppLocale(context, languageTag)
                    currentAppLocale = LocaleHelper.getCurrentAppLocale(context)
                    if (!LocaleHelper.usesFrameworkLocaleManager) {
                        refreshActivity(context)
                    }
                }
            }
        )
    }

    if (showCustomColorDialog) {
        CustomColorPickerDialog(
            initialColorArgb = customAccentArgb,
            onDismiss = { showCustomColorDialog = false },
            onColorConfirmed = { newArgb ->
                customAccentArgb = newArgb
                accentPresetKey = AccentPreset.CUSTOM.key
                dynamicColor = false
                activity?.setDynamicColor(false)
                activity?.setCustomAccentArgb(newArgb)
                activity?.setAccentPreset(AccentPreset.CUSTOM.key)
                showCustomColorDialog = false
            }
        )
    }

    if (showHeaderTitleDialog) {
        CustomHeaderDialog(
            initialTitle = customHeaderTitle,
            initialSubtitle = customHeaderSubtitle,
            onDismiss = { showHeaderTitleDialog = false },
            onConfirm = { newTitle, newSubtitle ->
                val finalTitle = newTitle.trim().ifEmpty { "GombokSU Enhanced" }
                val finalSub = newSubtitle.trim()
                customHeaderTitle = finalTitle
                customHeaderSubtitle = finalSub
                prefs.edit {
                    putString("custom_header_title", finalTitle)
                    putString("custom_header_subtitle", finalSub)
                }
                showHeaderTitleDialog = false
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialColorArgb: Int,
    onDismiss: () -> Unit,
    onColorConfirmed: (Int) -> Unit
) {
    val initialColor = Color(initialColorArgb)
    var red by remember { mutableFloatStateOf((initialColor.red * 255f).coerceIn(0f, 255f)) }
    var green by remember { mutableFloatStateOf((initialColor.green * 255f).coerceIn(0f, 255f)) }
    var blue by remember { mutableFloatStateOf((initialColor.blue * 255f).coerceIn(0f, 255f)) }

    val currentColor = Color(
        red = red.roundToInt().coerceIn(0, 255),
        green = green.roundToInt().coerceIn(0, 255),
        blue = blue.roundToInt().coerceIn(0, 255)
    )
    var hexText by remember(red, green, blue) {
        mutableStateOf(
            String.format(
                "%02X%02X%02X",
                red.roundToInt().coerceIn(0, 255),
                green.roundToInt().coerceIn(0, 255),
                blue.roundToInt().coerceIn(0, 255)
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.custom_color_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$hexText",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (currentColor.luminance() > 0.5f) Color.Black else Color.White
                    )
                }

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        val cleaned = input.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                        hexText = cleaned
                        if (cleaned.length == 6) {
                            runCatching {
                                val parsed = cleaned.toInt(16)
                                red = ((parsed shr 16) and 0xFF).toFloat()
                                green = ((parsed shr 8) and 0xFF).toFloat()
                                blue = (parsed and 0xFF).toFloat()
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.hex_color_code)) },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Red: ${red.roundToInt()}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = red,
                        onValueChange = { red = it },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFEF4444), activeTrackColor = Color(0xFFEF4444))
                    )
                    Text("Green: ${green.roundToInt()}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = green,
                        onValueChange = { green = it },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
                    )
                    Text("Blue: ${blue.roundToInt()}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = blue,
                        onValueChange = { blue = it },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onColorConfirmed(currentColor.toArgb()) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun CustomHeaderDialog(
    initialTitle: String,
    initialSubtitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var subtitle by remember { mutableStateOf(initialSubtitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.custom_header_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.header_title_label)) },
                    placeholder = { Text("GombokSU Enhanced") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text(stringResource(R.string.header_subtitle_label)) },
                    placeholder = { Text("Root & Module Manager") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, subtitle) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageBottomSheet(
    options: List<Pair<String, String>>,
    selectedLanguageTag: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = stringResource(R.string.select_language),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .semantics { heading() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .selectableGroup(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(options, key = { (tag, _) -> tag }) { (tag, displayName) ->
                val selected = tag == selectedLanguageTag
                ListItem(
                    headlineContent = { Text(displayName) },
                    trailingContent = {
                        RadioButton(selected = selected, onClick = null)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onLanguageSelected(tag) }
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    onReset: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.customization),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
        actions = {
            IconButton(onClick = onReset) {
                Icon(Icons.Filled.RestartAlt, contentDescription = stringResource(R.string.reset_to_default))
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

@Preview
@Composable
private fun CustomizationPreview() {
    CustomizationScreen(EmptyDestinationsNavigator)
}

