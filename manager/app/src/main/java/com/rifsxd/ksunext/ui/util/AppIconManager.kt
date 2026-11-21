package com.rifsxd.ksunext.ui.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.rifsxd.ksunext.R

enum class AppIconPreset(
    val key: String,
    val displayName: String,
    val subtitle: String,
    val aliasSuffix: String,
    @param:DrawableRes val foregroundMipmap: Int,
    @param:DrawableRes val monochromeMipmap: Int,
    @param:DrawableRes val vectorDrawable: Int
) {
    GOMBOKSU(
        key = "gomboksu",
        displayName = "GombokSU Enhanced",
        subtitle = "Comic Sans Logo (Normal & Material)",
        aliasSuffix = ".LauncherGombok",
        foregroundMipmap = R.mipmap.ic_launcher_foreground,
        monochromeMipmap = R.mipmap.ic_launcher_monochrome,
        vectorDrawable = R.drawable.ic_logo_gomboksu
    ),
    KSUNEXT(
        key = "ksunext",
        displayName = "KernelSU Next",
        subtitle = "Yin-Yang Next Logo (Normal & Material)",
        aliasSuffix = ".LauncherKsuNext",
        foregroundMipmap = R.mipmap.ic_launcher_ksunext_foreground,
        monochromeMipmap = R.mipmap.ic_launcher_ksunext_monochrome,
        vectorDrawable = R.drawable.ic_logo_ksunext
    ),
    KERNELSU(
        key = "kernelsu",
        displayName = "KernelSU",
        subtitle = "Classic Squares Logo (Normal & Material)",
        aliasSuffix = ".LauncherKsu",
        foregroundMipmap = R.mipmap.ic_launcher_ksu_foreground,
        monochromeMipmap = R.mipmap.ic_launcher_ksu_monochrome,
        vectorDrawable = R.drawable.ic_logo_kernelsu
    ),
    SKULL(
        key = "skull",
        displayName = "Skull & Crossbones ☠️",
        subtitle = "Google Noto Emoji (Normal & 1-Color Material)",
        aliasSuffix = ".LauncherSkull",
        foregroundMipmap = R.mipmap.ic_launcher_skull_foreground,
        monochromeMipmap = R.mipmap.ic_launcher_skull_monochrome,
        vectorDrawable = R.drawable.ic_logo_skull
    );

    companion object {
        fun fromKey(key: String?): AppIconPreset {
            return entries.firstOrNull { it.key == key } ?: GOMBOKSU
        }

        fun applyAppIcon(context: Context, selectedKey: String) {
            val selected = fromKey(selectedKey)
            val pm = context.packageManager
            val pkg = context.packageName

            entries.forEach { preset ->
                val component = ComponentName(pkg, pkg + preset.aliasSuffix)
                val targetState = if (preset == selected) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                runCatching {
                    if (pm.getComponentEnabledSetting(component) != targetState) {
                        pm.setComponentEnabledSetting(
                            component,
                            targetState,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
        }
    }
}
