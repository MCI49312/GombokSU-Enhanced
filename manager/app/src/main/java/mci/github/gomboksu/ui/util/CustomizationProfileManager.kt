package mci.github.gomboksu.ui.util

import android.content.Context
import androidx.core.content.edit
import mci.github.gomboksu.ui.MainActivity
import mci.github.gomboksu.ui.theme.AccentPreset
import mci.github.gomboksu.ui.theme.AppFontPreset
import org.json.JSONObject

data class CustomizationProfileData(
    val themeMode: Int,
    val dynamicColor: Boolean,
    val amoledMode: Boolean,
    val accentPresetKey: String,
    val customAccentArgb: Int,
    val cardRadiusDp: Int,
    val fontStyleKey: String,
    val appIconKey: String,
    val enableNavBar: Boolean,
    val navBarAutoHide: Boolean,
    val navBarStyle: Int,
    val navBarShowLabels: Boolean,
    val swipeNavigation: Boolean,
    val hapticFeedback: Boolean,
    val enableStartupAnimation: Boolean,
    val useBanner: Boolean,
    val showAboutDevice: Boolean,
    val autoExpandInfo: Boolean,
    val showContributors: Boolean,
    val showIssueReport: Boolean,
    val customHeaderTitle: String,
    val customHeaderSubtitle: String,
    val animationConfig: AnimationConfig
)

object CustomizationProfileManager {
    private const val PROFILE_FORMAT = "gomboksu_enhanced_profile"
    private const val PROFILE_VERSION = 1

    fun readCurrentProfile(context: Context): CustomizationProfileData {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return CustomizationProfileData(
            themeMode = prefs.getInt("theme_mode", 0),
            dynamicColor = prefs.getBoolean("enable_dynamic_color", false),
            amoledMode = prefs.getBoolean("enable_amoled", false),
            accentPresetKey = prefs.getString("accent_preset", AccentPreset.GOMBOK_BLUE.key)
                ?: AccentPreset.GOMBOK_BLUE.key,
            customAccentArgb = prefs.getInt("custom_accent_argb", 0xFF3B82F6.toInt()),
            cardRadiusDp = prefs.getInt("card_radius_dp", 16),
            fontStyleKey = prefs.getString("font_style", AppFontPreset.DEFAULT.key)
                ?: AppFontPreset.DEFAULT.key,
            appIconKey = prefs.getString("app_icon_key", AppIconPreset.GOMBOKSU.key)
                ?: AppIconPreset.GOMBOKSU.key,
            enableNavBar = prefs.getBoolean("enable_navbar", true),
            navBarAutoHide = prefs.getBoolean("navbar_auto_hide", true),
            navBarStyle = prefs.getInt("navbar_style", 0),
            navBarShowLabels = prefs.getBoolean("navbar_show_labels", false),
            swipeNavigation = prefs.getBoolean("enable_swipe_navigation", true),
            hapticFeedback = prefs.getBoolean("enable_haptic_feedback", true),
            enableStartupAnimation = prefs.getBoolean("enable_startup_animation", true),
            useBanner = prefs.getBoolean("use_banner", true),
            showAboutDevice = prefs.getBoolean("home_show_about_device", true),
            autoExpandInfo = prefs.getBoolean("home_auto_expand_info", false),
            showContributors = prefs.getBoolean("home_show_contributors", true),
            showIssueReport = prefs.getBoolean("home_show_issue_report", true),
            customHeaderTitle = prefs.getString("custom_header_title", "GombokSU Enhanced")
                ?: "GombokSU Enhanced",
            customHeaderSubtitle = prefs.getString("custom_header_subtitle", "") ?: "",
            animationConfig = AnimationConfig.fromPrefs(prefs)
        )
    }

    fun exportProfileJson(context: Context): String {
        val data = readCurrentProfile(context)
        val root = JSONObject().apply {
            put("format", PROFILE_FORMAT)
            put("version", PROFILE_VERSION)

            put("theme", JSONObject().apply {
                put("theme_mode", data.themeMode)
                put("enable_dynamic_color", data.dynamicColor)
                put("enable_amoled", data.amoledMode)
                put("accent_preset", data.accentPresetKey)
                put("custom_accent_argb", data.customAccentArgb)
                put("card_radius_dp", data.cardRadiusDp)
                put("font_style", data.fontStyleKey)
                put("app_icon_key", data.appIconKey)
            })

            put("navigation", JSONObject().apply {
                put("enable_navbar", data.enableNavBar)
                put("navbar_auto_hide", data.navBarAutoHide)
                put("navbar_style", data.navBarStyle)
                put("navbar_show_labels", data.navBarShowLabels)
                put("enable_swipe_navigation", data.swipeNavigation)
                put("enable_haptic_feedback", data.hapticFeedback)
                put("enable_startup_animation", data.enableStartupAnimation)
            })

            put("home", JSONObject().apply {
                put("use_banner", data.useBanner)
                put("home_show_about_device", data.showAboutDevice)
                put("home_auto_expand_info", data.autoExpandInfo)
                put("home_show_contributors", data.showContributors)
                put("home_show_issue_report", data.showIssueReport)
                put("custom_header_title", data.customHeaderTitle)
                put("custom_header_subtitle", data.customHeaderSubtitle)
            })

            put("animations", JSONObject().apply {
                put("anim_enabled", data.animationConfig.enabled)
                put("anim_bounce_multiplier", data.animationConfig.bounceMultiplier.toDouble())
                put("anim_speed_multiplier", data.animationConfig.speedMultiplier.toDouble())
                put("anim_page_transition", data.animationConfig.pageTransitionStyle)
                put("anim_splash_style", data.animationConfig.splashStyle)
                put("anim_card_press_bounce", data.animationConfig.cardPressBounce)
                put("anim_staggered_entrance", data.animationConfig.staggeredEntrance)
                put("anim_status_pulse_glow", data.animationConfig.statusPulseGlow)
                put("anim_navbar_icon_bounce", data.animationConfig.navBarIconBounce)
            })
        }
        return root.toString(2)
    }

    fun importProfileJson(
        context: Context,
        rawJson: String,
        activity: MainActivity?
    ): Result<CustomizationProfileData> = runCatching {
        val cleaned = rawJson.trim()
        require(cleaned.isNotEmpty()) { "Empty profile JSON" }

        val root = JSONObject(cleaned)
        val hasStructuredSections = root.has("theme") || root.has("navigation") ||
            root.has("home") || root.has("animations")
        val format = root.optString("format", "")
        require(format == PROFILE_FORMAT || hasStructuredSections) {
            "Not a valid GombokSU Enhanced profile"
        }

        val current = readCurrentProfile(context)
        val theme = root.optJSONObject("theme") ?: root
        val nav = root.optJSONObject("navigation") ?: root
        val home = root.optJSONObject("home") ?: root
        val animObj = root.optJSONObject("animations") ?: root

        val themeMode = theme.optInt("theme_mode", current.themeMode).coerceIn(0, 2)
        val dynamicColor = theme.optBoolean("enable_dynamic_color", current.dynamicColor)
        val amoledMode = theme.optBoolean("enable_amoled", current.amoledMode)
        val rawAccentKey = theme.optString("accent_preset", current.accentPresetKey)
        val accentPresetKey = AccentPreset.entries.firstOrNull { it.key == rawAccentKey }?.key
            ?: AccentPreset.GOMBOK_BLUE.key
        val customAccentArgb = theme.optInt("custom_accent_argb", current.customAccentArgb)
        val cardRadiusDp = theme.optInt("card_radius_dp", current.cardRadiusDp).coerceIn(4, 28)
        val rawFontKey = theme.optString("font_style", current.fontStyleKey)
        val fontStyleKey = AppFontPreset.entries.firstOrNull { it.key == rawFontKey }?.key
            ?: AppFontPreset.DEFAULT.key
        val rawIconKey = theme.optString("app_icon_key", current.appIconKey)
        val appIconKey = AppIconPreset.entries.firstOrNull { it.key == rawIconKey }?.key
            ?: AppIconPreset.GOMBOKSU.key

        val enableNavBar = nav.optBoolean("enable_navbar", current.enableNavBar)
        val navBarAutoHide = nav.optBoolean("navbar_auto_hide", current.navBarAutoHide)
        val navBarStyle = nav.optInt("navbar_style", current.navBarStyle).coerceIn(0, 2)
        val navBarShowLabels = nav.optBoolean("navbar_show_labels", current.navBarShowLabels)
        val swipeNavigation = nav.optBoolean("enable_swipe_navigation", current.swipeNavigation)
        val hapticFeedback = nav.optBoolean("enable_haptic_feedback", current.hapticFeedback)
        val enableStartupAnimation = nav.optBoolean("enable_startup_animation", current.enableStartupAnimation)

        val useBanner = home.optBoolean("use_banner", current.useBanner)
        val showAboutDevice = home.optBoolean("home_show_about_device", current.showAboutDevice)
        val autoExpandInfo = home.optBoolean("home_auto_expand_info", current.autoExpandInfo)
        val showContributors = home.optBoolean("home_show_contributors", current.showContributors)
        val showIssueReport = home.optBoolean("home_show_issue_report", current.showIssueReport)
        val customHeaderTitle = home.optString("custom_header_title", current.customHeaderTitle)
            .trim().take(48).ifEmpty { "GombokSU Enhanced" }
        val customHeaderSubtitle = home.optString("custom_header_subtitle", current.customHeaderSubtitle)
            .trim().take(64)

        val animConfig = AnimationConfig(
            enabled = animObj.optBoolean("anim_enabled", current.animationConfig.enabled),
            bounceMultiplier = animObj.optDouble(
                "anim_bounce_multiplier",
                current.animationConfig.bounceMultiplier.toDouble()
            ).toFloat().coerceIn(0f, 2.5f),
            speedMultiplier = animObj.optDouble(
                "anim_speed_multiplier",
                current.animationConfig.speedMultiplier.toDouble()
            ).toFloat().coerceIn(0.4f, 2.5f),
            pageTransitionStyle = animObj.optInt(
                "anim_page_transition",
                current.animationConfig.pageTransitionStyle
            ).coerceIn(0, 4),
            splashStyle = animObj.optInt(
                "anim_splash_style",
                current.animationConfig.splashStyle
            ).coerceIn(0, 3),
            cardPressBounce = animObj.optBoolean(
                "anim_card_press_bounce",
                current.animationConfig.cardPressBounce
            ),
            staggeredEntrance = animObj.optBoolean(
                "anim_staggered_entrance",
                current.animationConfig.staggeredEntrance
            ),
            statusPulseGlow = animObj.optBoolean(
                "anim_status_pulse_glow",
                current.animationConfig.statusPulseGlow
            ),
            navBarIconBounce = animObj.optBoolean(
                "anim_navbar_icon_bounce",
                current.animationConfig.navBarIconBounce
            )
        )

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit {
            putInt("theme_mode", themeMode)
            putBoolean("enable_dynamic_color", dynamicColor)
            putBoolean("enable_amoled", amoledMode)
            putString("accent_preset", accentPresetKey)
            putInt("custom_accent_argb", customAccentArgb)
            putInt("card_radius_dp", cardRadiusDp)
            putString("font_style", fontStyleKey)
            putString("app_icon_key", appIconKey)
            putBoolean("enable_navbar", enableNavBar)
            putBoolean("navbar_auto_hide", navBarAutoHide)
            putInt("navbar_style", navBarStyle)
            putBoolean("navbar_show_labels", navBarShowLabels)
            putBoolean("enable_swipe_navigation", swipeNavigation)
            putBoolean("enable_haptic_feedback", hapticFeedback)
            putBoolean("enable_startup_animation", enableStartupAnimation)
            putBoolean("use_banner", useBanner)
            putBoolean("home_show_about_device", showAboutDevice)
            putBoolean("home_auto_expand_info", autoExpandInfo)
            putBoolean("home_show_contributors", showContributors)
            putBoolean("home_show_issue_report", showIssueReport)
            putString("custom_header_title", customHeaderTitle)
            putString("custom_header_subtitle", customHeaderSubtitle)
        }

        val iconChanged = current.appIconKey != appIconKey
        activity?.let { act ->
            act.setThemeMode(themeMode)
            act.setDynamicColor(dynamicColor)
            act.setAmoledMode(amoledMode)
            act.setAccentPreset(accentPresetKey)
            act.setCustomAccentArgb(customAccentArgb)
            act.setCardRadiusDp(cardRadiusDp)
            act.setFontStyle(fontStyleKey)
            if (iconChanged) {
                act.setAppIconKey(appIconKey)
            } else {
                act.appIconKeyState.value = appIconKey
            }
            act.setNavBarEnabled(enableNavBar)
            act.setNavBarAutoHide(navBarAutoHide)
            act.setNavBarStyle(navBarStyle)
            act.setNavBarShowLabels(navBarShowLabels)
            act.setSwipeNavigation(swipeNavigation)
            act.setHapticFeedback(hapticFeedback)
            act.updateAnimationConfig(animConfig)
        }

        CustomizationProfileData(
            themeMode = themeMode,
            dynamicColor = dynamicColor,
            amoledMode = amoledMode,
            accentPresetKey = accentPresetKey,
            customAccentArgb = customAccentArgb,
            cardRadiusDp = cardRadiusDp,
            fontStyleKey = fontStyleKey,
            appIconKey = appIconKey,
            enableNavBar = enableNavBar,
            navBarAutoHide = navBarAutoHide,
            navBarStyle = navBarStyle,
            navBarShowLabels = navBarShowLabels,
            swipeNavigation = swipeNavigation,
            hapticFeedback = hapticFeedback,
            enableStartupAnimation = enableStartupAnimation,
            useBanner = useBanner,
            showAboutDevice = showAboutDevice,
            autoExpandInfo = autoExpandInfo,
            showContributors = showContributors,
            showIssueReport = showIssueReport,
            customHeaderTitle = customHeaderTitle,
            customHeaderSubtitle = customHeaderSubtitle,
            animationConfig = animConfig
        )
    }
}
