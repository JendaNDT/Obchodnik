package cz.obchodnik.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner-radius tokens; differ per theme (sharp Mono, rounded Aurora). */
@Immutable
data class ObchodnikRadii(
    val radius: Dp,
    val radiusSm: Dp,
    val chip: Dp,
)

fun radiiFor(theme: ThemeChoice): ObchodnikRadii = when (theme) {
    ThemeChoice.TERMINAL -> ObchodnikRadii(14.dp, 10.dp, 9.dp)
    ThemeChoice.AURORA -> ObchodnikRadii(20.dp, 15.dp, 999.dp)
    ThemeChoice.MONO -> ObchodnikRadii(4.dp, 4.dp, 4.dp)
}

val LocalObchodnikColors = staticCompositionLocalOf { terminalColors(AccentChoice.BLUE.color) }
val LocalObchodnikRadii = staticCompositionLocalOf { radiiFor(ThemeChoice.TERMINAL) }

/** Single access point for design tokens: `Obchodnik.colors`, `Obchodnik.radii`. */
object Obchodnik {
    val colors: ObchodnikColors
        @Composable get() = LocalObchodnikColors.current
    val radii: ObchodnikRadii
        @Composable get() = LocalObchodnikRadii.current
}

@Composable
fun ObchodnikTheme(
    theme: ThemeChoice = ThemeChoice.TERMINAL,
    accent: AccentChoice = AccentChoice.BLUE,
    content: @Composable () -> Unit,
) {
    val colors = colorsFor(theme, accent.color)
    val radii = radiiFor(theme)
    val uiFont = if (theme == ThemeChoice.MONO) JetBrainsMono else HankenGrotesk

    val scheme = darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        background = colors.bg,
        onBackground = colors.text,
        surface = colors.surface,
        onSurface = colors.text,
        surfaceVariant = colors.surface2,
        onSurfaceVariant = colors.text2,
        outline = colors.borderStrong,
        error = colors.down,
    )

    CompositionLocalProvider(
        LocalObchodnikColors provides colors,
        LocalObchodnikRadii provides radii,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = appTypography(uiFont),
            content = content,
        )
    }
}
