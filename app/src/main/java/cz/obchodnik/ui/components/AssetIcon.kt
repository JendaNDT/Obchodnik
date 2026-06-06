package cz.obchodnik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.ui.theme.JetBrainsMono

@Composable
fun AssetIcon(
    symbol: String,
    colorHex: String?,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
) {
    val color = colorFromHex(colorHex) ?: Color(0xFF3B82F6)
    val mono = if (symbol.length > 4) symbol.take(3) else symbol.take(1)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.34f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = mono,
            color = color,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = if (mono.length > 1) 10.sp else 16.sp,
        )
    }
}

fun colorFromHex(value: String?): Color? =
    runCatching {
        if (value.isNullOrBlank()) null else Color(android.graphics.Color.parseColor(value))
    }.getOrNull()
