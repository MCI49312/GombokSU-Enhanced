package mci.github.gomboksu.ui.theme

import androidx.compose.ui.graphics.Color

val PRIMARY = Color(0xFF8AADF4)           // Catppuccin Blue
val PRIMARY_LIGHT = Color(0xFFB7BDF8)     // Catppuccin Lavender
val SECONDARY_LIGHT = Color(0xFFA6DA95)   // Catppuccin Green

val PRIMARY_DARK = Color(0xFF7DC4E4)      // Catppuccin Sky
val SECONDARY_DARK = Color(0xFFF5BDE6)    // Catppuccin Pink

val AMOLED_BLACK = Color(0xFF000000)      // Pure black for AMOLED

val DARK_PURPLE = Color(0xFF6E6CB6)       // Catppuccin Mauve (dark purple)
val DARK_GREY = Color(0xFF363A4F)         // Catppuccin Surface (dark grey)

val GREEN = Color(0xFF4CAF50)             // Green
val RED = Color(0xFFF44336)               // Red
val YELLOW = Color(0xFFFFEB3B)            // Yellow
val ORANGE = Color(0xFFFF9800)            // Orange

enum class AccentPreset(
    val key: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
) {
    DEVICE_ACCENT("device_accent", "Device Accent", Color(0xFF3B82F6), Color(0xFF60A5FA), Color(0xFF38BDF8)),
    GOMBOK_BLUE("gombok_blue", "Gombok Blue", Color(0xFF3B82F6), Color(0xFF60A5FA), Color(0xFF38BDF8)),
    CATPPUCCIN("catppuccin", "Catppuccin Blue", Color(0xFF8AADF4), Color(0xFF7DC4E4), Color(0xFFF5BDE6)),
    EMERALD("emerald", "Emerald Green", Color(0xFF10B981), Color(0xFF34D399), Color(0xFFA7F3D0)),
    CRIMSON("crimson", "Crimson Red", Color(0xFFEF4444), Color(0xFFF87171), Color(0xFFFCA5A5)),
    AMETHYST("amethyst", "Amethyst Purple", Color(0xFFA855F7), Color(0xFFC084FC), Color(0xFFE879F9)),
    SUNSET("sunset", "Sunset Orange", Color(0xFFF97316), Color(0xFFFB923C), Color(0xFFFACC15)),
    SAKURA("sakura", "Sakura Pink", Color(0xFFEC4899), Color(0xFFF472B6), Color(0xFFF9A8D4)),
    CYBER_GOLD("cyber_gold", "Cyber Gold", Color(0xFFEAB308), Color(0xFFFACC15), Color(0xFFFDE047)),
    OCEAN_TEAL("ocean_teal", "Ocean Teal", Color(0xFF14B8A6), Color(0xFF2DD4BF), Color(0xFF5EEAD4)),
    NORD_FROST("nord_frost", "Nord Frost", Color(0xFF88C0D0), Color(0xFF81A1C1), Color(0xFF5E81AC)),
    DRACULA("dracula", "Dracula Violet", Color(0xFFBD93F9), Color(0xFFFF79C6), Color(0xFF8BE9FD)),
    MINT("mint", "Mint Leaf", Color(0xFF22C55E), Color(0xFF4ADE80), Color(0xFF86EFAC)),
    CUSTOM("custom", "Custom RGB / Hex", Color(0xFF6366F1), Color(0xFF818CF8), Color(0xFFA5B4FC));

    companion object {
        fun fromKey(key: String?): AccentPreset {
            return entries.firstOrNull { it.key == key } ?: GOMBOK_BLUE
        }
    }
}
