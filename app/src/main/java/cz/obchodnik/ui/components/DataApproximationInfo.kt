package cz.obchodnik.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.domain.AssetType
import cz.obchodnik.ui.theme.Obchodnik

@Composable
fun DataApproximationDialog(
    assetType: AssetType,
    onDismiss: () -> Unit,
) {
    val c = Obchodnik.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (assetType) {
                    AssetType.INDEX -> "Index přes ETF"
                    AssetType.COMMODITY -> "Komoditní data"
                    else -> "Poznámka k datům"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                DataApproximationParagraph(
                    text = when (assetType) {
                        AssetType.INDEX ->
                            "S&P 500, Nasdaq 100 a Dow Jones sledujeme přes ETF zástupce SPY, QQQ a DIA."
                        AssetType.COMMODITY ->
                            "Komodity načítáme přes Alpha Vantage jako referenční denní data."
                        else ->
                            "Některá tržní data mohou být dopočítaná nebo zpožděná podle zdroje."
                    },
                )
                DataApproximationParagraph(
                    text = when (assetType) {
                        AssetType.INDEX ->
                            "Nejde o oficiální hodnotu indexu. Výsledek se může lišit kvůli ETF, poplatkům, obchodním hodinám a převodu měny."
                        AssetType.COMMODITY ->
                            "Nejde o živý intraday feed. Hodnota se může aktualizovat se zpožděním a v CZK je dopočítaná přes kurz USD/CZK."
                        else ->
                            "Hodnota v CZK je dopočítaná přes dostupný měnový kurz."
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Rozumím", color = c.accent, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun DataApproximationParagraph(text: String) {
    Text(
        text = text,
        color = Obchodnik.colors.text2,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}
