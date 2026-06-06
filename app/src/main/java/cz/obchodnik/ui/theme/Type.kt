package cz.obchodnik.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import cz.obchodnik.R

/**
 * Typography.
 *
 * The design package specifies Hanken Grotesk (UI) + JetBrains Mono (numbers).
 * Both are loaded via Downloadable Fonts (Google Fonts provider). If the
 * provider is unavailable the system falls back to the default platform font,
 * so the app keeps working without bundled font binaries.
 */
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val hankenGroteskFont = GoogleFont("Hanken Grotesk")
private val jetBrainsMonoFont = GoogleFont("JetBrains Mono")

val HankenGrotesk: FontFamily = FontFamily(
    Font(googleFont = hankenGroteskFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = hankenGroteskFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = hankenGroteskFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = hankenGroteskFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = hankenGroteskFont, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold),
)

/** Monospace family for prices, percentages and tickers (prevents digit jitter). */
val JetBrainsMono: FontFamily = FontFamily(
    Font(googleFont = jetBrainsMonoFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = jetBrainsMonoFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = jetBrainsMonoFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = jetBrainsMonoFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
)

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
