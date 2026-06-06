package cz.obchodnik.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: return@Canvas
        val max = values.maxOrNull() ?: return@Canvas
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        val line = Path()
        values.forEachIndexed { index, value ->
            val x = (index.toFloat() / (values.lastIndex).toFloat()) * size.width
            val y = size.height - (((value - min) / span).toFloat() * size.height)
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent)),
        )
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val last = values.last()
        val lastY = size.height - (((last - min) / span).toFloat() * size.height)
        drawCircle(color = color, radius = 2.2.dp.toPx(), center = Offset(size.width, lastY))
    }
}
