package cz.obchodnik.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Custom design-token palette mirroring the handoff design package.
 * These tokens drive the whole UI; Material3's ColorScheme is derived from them
 * in [ObchodnikTheme] so standard components also pick up the theme.
 */
@Immutable
data class ObchodnikColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val onAccent: Color,
    val up: Color,
    val down: Color,
)

/** Shared semantic colors (identical across all themes). */
val PriceUp = Color(0xFF16C784)
val PriceDown = Color(0xFFEA3943)

/** Accent palette the user can pick from. */
enum class AccentChoice(val color: Color, val labelCs: String) {
    BLUE(Color(0xFF3B82F6), "Modrá"),
    GREEN(Color(0xFF16C784), "Zelená"),
    ORANGE(Color(0xFFF7931A), "Oranžová"),
    PURPLE(Color(0xFFA78BFA), "Fialová"),
}

/** Three dark themes from the handoff. */
enum class ThemeChoice(val labelCs: String) {
    TERMINAL("Terminal"),
    AURORA("Aurora"),
    MONO("Mono"),
}

private val OnAccent = Color(0xFFFFFFFF)

fun terminalColors(accent: Color) = ObchodnikColors(
    bg = Color(0xFF07090C),
    surface = Color(0xFF0E1218),
    surface2 = Color(0xFF171C24),
    border = Color(0x12FFFFFF),
    borderStrong = Color(0x26FFFFFF),
    text = Color(0xFFE7EAEF),
    text2 = Color(0xFF99A2AF),
    text3 = Color(0xFF5C6573),
    accent = accent,
    onAccent = OnAccent,
    up = PriceUp,
    down = PriceDown,
)

fun auroraColors(accent: Color) = ObchodnikColors(
    bg = Color(0xFF090A14),
    surface = Color(0xFF15172A),
    surface2 = Color(0xFF1F2340),
    border = Color(0x1A96A5FF),
    borderStrong = Color(0x3896A5FF),
    text = Color(0xFFEDEEF7),
    text2 = Color(0xFFA6ACCA),
    text3 = Color(0xFF6C7193),
    accent = accent,
    onAccent = OnAccent,
    up = PriceUp,
    down = PriceDown,
)

fun monoColors(accent: Color) = ObchodnikColors(
    bg = Color(0xFF0A0A0A),
    surface = Color(0xFF121212),
    surface2 = Color(0xFF1D1D1D),
    border = Color(0x14FFFFFF),
    borderStrong = Color(0x2EFFFFFF),
    text = Color(0xFFEDEDED),
    text2 = Color(0xFF8C8C8C),
    text3 = Color(0xFF565656),
    accent = accent,
    onAccent = OnAccent,
    up = PriceUp,
    down = PriceDown,
)

fun colorsFor(theme: ThemeChoice, accent: Color): ObchodnikColors = when (theme) {
    ThemeChoice.TERMINAL -> terminalColors(accent)
    ThemeChoice.AURORA -> auroraColors(accent)
    ThemeChoice.MONO -> monoColors(accent)
}
