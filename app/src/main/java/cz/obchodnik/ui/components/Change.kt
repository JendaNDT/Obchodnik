package cz.obchodnik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.core.format.MarketFormatters
import cz.obchodnik.ui.theme.JetBrainsMono
import cz.obchodnik.ui.theme.Obchodnik

@Composable
fun Change(
    value: Double?,
    modifier: Modifier = Modifier,
    chip: Boolean = false,
) {
    val c = Obchodnik.colors
    val up = (value ?: 0.0) >= 0.0
    val color = if (up) c.up else c.down
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = MarketFormatters.percent(value),
                color = color,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
        }
    }
    if (chip) {
        Row(
            modifier = modifier
                .background(color.copy(alpha = 0.13f), RoundedCornerShape(Obchodnik.radii.chip))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            content()
        }
    } else {
        Row(modifier = modifier) {
            content()
        }
    }
}
