package mci.github.gomboksu.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import mci.github.gomboksu.R

val Typography = androidx.compose.material3.Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

private fun singleFontFamily(resId: Int): FontFamily = FontFamily(
    Font(resId, FontWeight.Normal),
    Font(resId, FontWeight.Medium),
    Font(resId, FontWeight.SemiBold),
    Font(resId, FontWeight.Bold),
    Font(resId, FontWeight.Black)
)

private val ComicSansFamily = FontFamily(
    Font(R.font.comic_sans, FontWeight.Normal),
    Font(R.font.comic_sans, FontWeight.Medium),
    Font(R.font.comic_sans_bold, FontWeight.SemiBold),
    Font(R.font.comic_sans_bold, FontWeight.Bold),
    Font(R.font.comic_sans_bold, FontWeight.Black)
)

enum class AppFontPreset(
    val key: String,
    val displayName: String,
    val category: String,
    val fontFamily: FontFamily
) {
    DEFAULT("default", "System Default (Roboto)", "System", FontFamily.Default),
    COMIC_SANS("comic_sans", "Comic Sans", "Handwriting & Fun", ComicSansFamily),
    INTER("inter", "Inter", "Modern Sans", singleFontFamily(R.font.inter_font)),
    POPPINS("poppins", "Poppins", "Modern Sans", singleFontFamily(R.font.poppins)),
    MONTSERRAT("montserrat", "Montserrat", "Modern Sans", singleFontFamily(R.font.montserrat)),
    SPACE_GROTESK("space_grotesk", "Space Grotesk", "Modern Sans", singleFontFamily(R.font.space_grotesk)),
    QUICKSAND("quicksand", "Quicksand", "Modern Sans", singleFontFamily(R.font.quicksand)),
    COMFORTAA("comfortaa", "Comfortaa", "Modern Sans", singleFontFamily(R.font.comfortaa)),
    FREDOKA("fredoka", "Fredoka", "Modern Sans", singleFontFamily(R.font.fredoka)),
    RUBIK("rubik", "Rubik", "Modern Sans", singleFontFamily(R.font.rubik)),
    LEXEND("lexend", "Lexend", "Modern Sans", singleFontFamily(R.font.lexend)),
    JOSEFIN_SANS("josefin_sans", "Josefin Sans", "Modern Sans", singleFontFamily(R.font.josefin_sans)),
    EXO_2("exo_2", "Exo 2", "Modern Sans", singleFontFamily(R.font.exo_2)),
    TITILLIUM_WEB("titillium_web", "Titillium Web", "Modern Sans", singleFontFamily(R.font.titillium_web)),
    SYNE("syne", "Syne", "Modern Sans", singleFontFamily(R.font.syne)),
    JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono", "Monospace & Coding", singleFontFamily(R.font.jetbrains_mono)),
    FIRA_CODE("fira_code", "Fira Code", "Monospace & Coding", singleFontFamily(R.font.fira_code)),
    SPACE_MONO("space_mono", "Space Mono", "Monospace & Coding", singleFontFamily(R.font.space_mono)),
    UBUNTU_MONO("ubuntu_mono", "Ubuntu Mono", "Monospace & Coding", singleFontFamily(R.font.ubuntu_mono)),
    SYSTEM_MONOSPACE("monospace", "Droid Sans Mono (System)", "Monospace & Coding", FontFamily.Monospace),
    VT323("vt323", "VT323 Terminal", "Retro & Gaming", singleFontFamily(R.font.vt323)),
    PRESS_START_2P("press_start_2p", "Press Start 2P (8-Bit)", "Retro & Gaming", singleFontFamily(R.font.press_start_2p)),
    SILKSCREEN("silkscreen", "Silkscreen Pixel", "Retro & Gaming", singleFontFamily(R.font.silkscreen)),
    ORBITRON("orbitron", "Orbitron Sci-Fi", "Futuristic & Tech", singleFontFamily(R.font.orbitron)),
    AUDIOWIDE("audiowide", "Audiowide Cyber", "Futuristic & Tech", singleFontFamily(R.font.audiowide)),
    CHAKRA_PETCH("chakra_petch", "Chakra Petch", "Futuristic & Tech", singleFontFamily(R.font.chakra_petch)),
    OXANIUM("oxanium", "Oxanium", "Futuristic & Tech", singleFontFamily(R.font.oxanium)),
    RAJDHANI("rajdhani", "Rajdhani", "Futuristic & Tech", singleFontFamily(R.font.rajdhani)),
    TEKO("teko", "Teko", "Display & Poster", singleFontFamily(R.font.teko)),
    BEBAS_NEUE("bebas_neue", "Bebas Neue", "Display & Poster", singleFontFamily(R.font.bebas_neue)),
    ANTON("anton", "Anton", "Display & Poster", singleFontFamily(R.font.anton)),
    OSWALD("oswald", "Oswald", "Display & Poster", singleFontFamily(R.font.oswald)),
    RIGHTEOUS("righteous", "Righteous", "Display & Poster", singleFontFamily(R.font.righteous)),
    BUNGEE("bungee", "Bungee", "Display & Poster", singleFontFamily(R.font.bungee)),
    BLACK_OPS_ONE("black_ops_one", "Black Ops One", "Display & Poster", singleFontFamily(R.font.black_ops_one)),
    BANGERS("bangers", "Bangers Comic", "Display & Poster", singleFontFamily(R.font.bangers)),
    MONOTON("monoton", "Monoton Neon", "Display & Poster", singleFontFamily(R.font.monoton)),
    CREEPSTER("creepster", "Creepster Horror", "Display & Poster", singleFontFamily(R.font.creepster)),
    SPECIAL_ELITE("special_elite", "Special Elite Typewriter", "Serif & Classic", singleFontFamily(R.font.special_elite)),
    PLAYFAIR_DISPLAY("playfair_display", "Playfair Display", "Serif & Classic", singleFontFamily(R.font.playfair_display)),
    DM_SERIF_DISPLAY("dm_serif_display", "DM Serif Display", "Serif & Classic", singleFontFamily(R.font.dm_serif_display)),
    ABRIL_FATFACE("abril_fatface", "Abril Fatface", "Serif & Classic", singleFontFamily(R.font.abril_fatface)),
    CINZEL("cinzel", "Cinzel Roman", "Serif & Classic", singleFontFamily(R.font.cinzel)),
    SYSTEM_SERIF("serif", "Noto Serif (System)", "Serif & Classic", FontFamily.Serif),
    PACIFICO("pacifico", "Pacifico Script", "Handwriting & Fun", singleFontFamily(R.font.pacifico)),
    LOBSTER("lobster", "Lobster", "Handwriting & Fun", singleFontFamily(R.font.lobster)),
    CAVEAT("caveat", "Caveat Brush", "Handwriting & Fun", singleFontFamily(R.font.caveat)),
    PATRICK_HAND("patrick_hand", "Patrick Hand", "Handwriting & Fun", singleFontFamily(R.font.patrick_hand)),
    PERMANENT_MARKER("permanent_marker", "Permanent Marker", "Handwriting & Fun", singleFontFamily(R.font.permanent_marker)),
    SHADOWS_INTO_LIGHT("shadows_into_light", "Shadows Into Light", "Handwriting & Fun", singleFontFamily(R.font.shadows_into_light)),
    INDIE_FLOWER("indie_flower", "Indie Flower", "Handwriting & Fun", singleFontFamily(R.font.indie_flower)),
    ARCHITECTS_DAUGHTER("architects_daughter", "Architects Daughter", "Handwriting & Fun", singleFontFamily(R.font.architects_daughter)),
    GLORIA_HALLELUJAH("gloria_hallelujah", "Gloria Hallelujah", "Handwriting & Fun", singleFontFamily(R.font.gloria_hallelujah)),
    AMATIC_SC("amatic_sc", "Amatic SC", "Handwriting & Fun", singleFontFamily(R.font.amatic_sc)),
    DANCING_SCRIPT("dancing_script", "Dancing Script", "Handwriting & Fun", singleFontFamily(R.font.dancing_script)),
    SATISFY("satisfy", "Satisfy Cursive", "Handwriting & Fun", singleFontFamily(R.font.satisfy)),
    KAUSHAN_SCRIPT("kaushan_script", "Kaushan Script", "Handwriting & Fun", singleFontFamily(R.font.kaushan_script));

    companion object {
        fun fromKey(key: String?): AppFontPreset {
            if (key == "sans") return INTER
            return entries.firstOrNull { it.key == key } ?: DEFAULT
        }
    }
}
