package com.rifsxd.ksunext.ui.theme

import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PRIMARY,
    secondary = PRIMARY_DARK,
    tertiary = SECONDARY_DARK
)

private val LightColorScheme = lightColorScheme(
    primary = PRIMARY,
    secondary = PRIMARY_LIGHT,
    tertiary = SECONDARY_LIGHT
)

fun Color.blend(other: Color, ratio: Float): Color {
    val inverse = 1f - ratio
    return Color(
        red = red * inverse + other.red * ratio,
        green = green * inverse + other.green * ratio,
        blue = blue * inverse + other.blue * ratio,
        alpha = alpha
    )
}

fun buildCustomColorScheme(
    seedPrimary: Color,
    seedSecondary: Color,
    seedTertiary: Color,
    darkTheme: Boolean,
    amoledMode: Boolean
): ColorScheme {
    val onPrimaryColor = if (seedPrimary.luminance() > 0.5f) Color(0xFF111827) else Color.White
    return if (darkTheme) {
        val bg = if (amoledMode) AMOLED_BLACK else Color(0xFF111318).blend(seedPrimary, 0.05f)
        val surf = if (amoledMode) AMOLED_BLACK else Color(0xFF181A20).blend(seedPrimary, 0.06f)
        val surfVar = if (amoledMode) DARK_GREY.blend(AMOLED_BLACK, 0.72f).blend(seedPrimary, 0.14f)
            else Color(0xFF262932).blend(seedPrimary, 0.14f)
        val primaryContainer = seedPrimary.blend(Color.Black, 0.55f)
        val onPrimaryContainer = seedPrimary.blend(Color.White, 0.78f)
        val secondaryContainer = seedSecondary.blend(Color.Black, 0.58f)
        val onSecondaryContainer = seedSecondary.blend(Color.White, 0.80f)
        val tertiaryContainer = seedTertiary.blend(Color.Black, 0.58f)
        val onTertiaryContainer = seedTertiary.blend(Color.White, 0.80f)

        darkColorScheme(
            primary = seedPrimary,
            onPrimary = onPrimaryColor,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = seedSecondary,
            onSecondary = if (seedSecondary.luminance() > 0.5f) Color.Black else Color.White,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = seedTertiary,
            onTertiary = if (seedTertiary.luminance() > 0.5f) Color.Black else Color.White,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = bg,
            surface = surf,
            surfaceVariant = surfVar,
            surfaceContainer = surfVar,
            surfaceContainerLow = surf.blend(surfVar, 0.4f),
            surfaceContainerLowest = bg,
            surfaceContainerHigh = surfVar.blend(Color.White, 0.06f),
            surfaceContainerHighest = surfVar.blend(Color.White, 0.12f)
        )
    } else {
        val bg = Color(0xFFF8FAFC).blend(seedPrimary, 0.04f)
        val surf = Color(0xFFFFFFFF).blend(seedPrimary, 0.03f)
        val surfVar = Color(0xFFE2E8F0).blend(seedPrimary, 0.12f)
        val primaryContainer = seedPrimary.blend(Color.White, 0.75f)
        val onPrimaryContainer = seedPrimary.blend(Color.Black, 0.75f)
        val secondaryContainer = seedSecondary.blend(Color.White, 0.78f)
        val onSecondaryContainer = seedSecondary.blend(Color.Black, 0.75f)
        val tertiaryContainer = seedTertiary.blend(Color.White, 0.78f)
        val onTertiaryContainer = seedTertiary.blend(Color.Black, 0.75f)

        lightColorScheme(
            primary = seedPrimary,
            onPrimary = onPrimaryColor,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = seedSecondary,
            onSecondary = if (seedSecondary.luminance() > 0.5f) Color.Black else Color.White,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = seedTertiary,
            onTertiary = if (seedTertiary.luminance() > 0.5f) Color.Black else Color.White,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = bg,
            surface = surf,
            surfaceVariant = surfVar,
            surfaceContainer = surfVar.blend(Color.White, 0.4f),
            surfaceContainerLow = surf.blend(surfVar, 0.35f),
            surfaceContainerLowest = Color.White,
            surfaceContainerHigh = surfVar,
            surfaceContainerHighest = surfVar.blend(Color.Black, 0.05f)
        )
    }
}

fun resolveAppColorScheme(
    context: Context,
    darkTheme: Boolean,
    dynamicColor: Boolean,
    amoledMode: Boolean,
    accentPresetKey: String = "gombok_blue",
    customAccentArgb: Int = 0xFF3B82F6.toInt()
): ColorScheme {
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (amoledMode && darkTheme) {
                dynamicScheme.copy(
                    background = AMOLED_BLACK,
                    surface = AMOLED_BLACK,
                    surfaceVariant = dynamicScheme.surfaceVariant.blend(AMOLED_BLACK, 0.6f),
                    surfaceContainer = dynamicScheme.surfaceContainer.blend(AMOLED_BLACK, 0.6f),
                    surfaceContainerLow = dynamicScheme.surfaceContainerLow.blend(AMOLED_BLACK, 0.6f),
                    surfaceContainerLowest = dynamicScheme.surfaceContainerLowest.blend(AMOLED_BLACK, 0.6f),
                    surfaceContainerHigh = dynamicScheme.surfaceContainerHigh.blend(AMOLED_BLACK, 0.6f),
                    surfaceContainerHighest = dynamicScheme.surfaceContainerHighest.blend(AMOLED_BLACK, 0.6f)
                )
            } else {
                dynamicScheme
            }
        }
        else -> {
            val preset = AccentPreset.fromKey(accentPresetKey)
            val primary = if (preset == AccentPreset.CUSTOM) Color(customAccentArgb) else preset.primary
            val secondary = if (preset == AccentPreset.CUSTOM) primary.blend(Color.White, 0.25f) else preset.secondary
            val tertiary = if (preset == AccentPreset.CUSTOM) primary.blend(Color(0xFF38BDF8), 0.35f) else preset.tertiary
            buildCustomColorScheme(
                seedPrimary = primary,
                seedSecondary = secondary,
                seedTertiary = tertiary,
                darkTheme = darkTheme,
                amoledMode = amoledMode
            )
        }
    }
}

@Composable
fun KernelSUTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: Int = 0, // 0 = System, 1 = Light, 2 = Dark
    dynamicColor: Boolean = false,
    amoledMode: Boolean = false,
    accentPresetKey: String = "gombok_blue",
    customAccentArgb: Int = 0xFF3B82F6.toInt(),
    cardRadiusDp: Int = 16,
    fontStyleKey: String = "default",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val effectiveDarkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> darkTheme
    }

    val colorScheme = resolveAppColorScheme(
        context = context,
        darkTheme = effectiveDarkTheme,
        dynamicColor = dynamicColor,
        amoledMode = amoledMode,
        accentPresetKey = accentPresetKey,
        customAccentArgb = customAccentArgb
    )

    val fontFamily = when (fontStyleKey) {
        "monospace" -> FontFamily.Monospace
        "serif" -> FontFamily.Serif
        "sans" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }

    val customTypography = Typography.copy(
        displayLarge = Typography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = Typography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = Typography.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = Typography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = Typography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = Typography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = Typography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = Typography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = Typography.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = Typography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = Typography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = Typography.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = Typography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = Typography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = Typography.labelSmall.copy(fontFamily = fontFamily)
    )

    val r = cardRadiusDp.coerceIn(0, 32)
    val customShapes = Shapes(
        extraSmall = RoundedCornerShape((r / 3).coerceAtLeast(2).dp),
        small = RoundedCornerShape((r / 2).coerceAtLeast(4).dp),
        medium = RoundedCornerShape(r.dp),
        large = RoundedCornerShape((r + 4).dp),
        extraLarge = RoundedCornerShape((r + 12).dp)
    )

    SystemBarStyle(
        darkMode = effectiveDarkTheme
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = customTypography,
        shapes = customShapes,
        content = content
    )
}

@Composable
private fun SystemBarStyle(
    darkMode: Boolean,
    statusBarScrim: Color = Color.Transparent,
    navigationBarScrim: Color = Color.Transparent,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                statusBarScrim.toArgb(),
                statusBarScrim.toArgb(),
            ) { darkMode },
            navigationBarStyle = when {
                darkMode -> SystemBarStyle.dark(
                    navigationBarScrim.toArgb()
                )

                else -> SystemBarStyle.light(
                    navigationBarScrim.toArgb(),
                    navigationBarScrim.toArgb(),
                )
            }
        )
    }
}
