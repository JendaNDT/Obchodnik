package cz.obchodnik.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography.
 *
 * The design package specifies Hanken Grotesk (UI) + JetBrains Mono (numbers).
 * For now we use the built-in sans/monospace families so the project builds and
 * runs with zero bundled font binaries and no Google Fonts certificate file.
 *
 * TODO(fonts): switch to the real fonts via Android Studio →
 * res/font → "Add font" → Downloadable Font → pick "Hanken Grotesk" and
 * "JetBrains Mono". Android Studio auto-generates res/values/font_certs.xml.
 * Then set [HankenGrotesk] / [JetBrainsMono] to those FontFamily definitions.
 */
val HankenGrotesk: FontFamily = FontFamily.SansSerif

/** Monospace family for prices, percentages and tickers (prevents digit jitter). */
val JetBrainsMono: FontFamily = FontFamily.Monospace

fun appTypography(ui: FontFamily = HankenGrotesk): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = ui),
        displayMedium = base.displayMedium.copy(fontFamily = ui),
        displaySmall = base.displaySmall.copy(fontFamily = ui),
        headlineLarge = base.headlineLarge.copy(fontFamily = ui, fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = ui, fontWeight = FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = ui, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = ui, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = ui, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = ui, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = ui),
        bodyMedium = base.bodyMedium.copy(fontFamily = ui),
        bodySmall = base.bodySmall.copy(fontFamily = ui),
        labelLarge = base.labelLarge.copy(fontFamily = ui, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = ui),
        labelSmall = base.labelSmall.copy(fontFamily = ui),
    )
}

/** Convenience numeric style helper used for prices/percentages. */
@Composable
fun monoStyle(size: Int, weight: FontWeight = FontWeight.SemiBold): TextStyle =
    TextStyle(fontFamily = JetBrainsMono, fontWeight = weight, fontSize = size.sp)
