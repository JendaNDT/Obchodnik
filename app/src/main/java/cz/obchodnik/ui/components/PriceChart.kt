package cz.obchodnik.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.PricePoint

@Composable
fun LinePriceChart(
    points: List<PricePoint>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val prices = points.map { it.price }
        val min = prices.minOrNull() ?: return@Canvas
        val max = prices.maxOrNull() ?: return@Canvas
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        val topPad = 12.dp.toPx()
        val bottomPad = 18.dp.toPx()
        val chartHeight = size.height - topPad - bottomPad
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = (index.toFloat() / points.lastIndex.toFloat()) * size.width
            val y = topPad + chartHeight - (((point.price - min) / span).toFloat() * chartHeight)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val area = Path().apply {
            addPath(path)
            lineTo(size.width, topPad + chartHeight)
            lineTo(0f, topPad + chartHeight)
            close()
        }
        repeat(5) { i ->
            val y = topPad + (chartHeight / 4f) * i
            drawLine(
                color = Color.White.copy(alpha = 0.07f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.24f), Color.Transparent)))
        drawPath(path, color = color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = points.last().price
        val y = topPad + chartHeight - (((last - min) / span).toFloat() * chartHeight)
        drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(size.width, y))
    }
}

@Composable
fun CandlePriceChart(
    candles: List<Candle>,
    upColor: Color,
    downColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (candles.size < 2) return@Canvas
        val min = candles.minOf { it.low }
        val max = candles.maxOf { it.high }
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        val topPad = 12.dp.toPx()
        val bottomPad = 18.dp.toPx()
        val chartHeight = size.height - topPad - bottomPad
        fun y(value: Double): Float =
            topPad + chartHeight - (((value - min) / span).toFloat() * chartHeight)

        repeat(5) { i ->
            val gy = topPad + (chartHeight / 4f) * i
            drawLine(
                color = Color.White.copy(alpha = 0.07f),
                start = Offset(0f, gy),
                end = Offset(size.width, gy),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val step = size.width / candles.size
        val bodyWidth = (step * 0.58f).coerceAtLeast(2.dp.toPx())
        candles.forEachIndexed { index, candle ->
            val cx = step * index + step / 2f
            val color = if (candle.close >= candle.open) upColor else downColor
            val openY = y(candle.open)
            val closeY = y(candle.close)
            val highY = y(candle.high)
            val lowY = y(candle.low)
            drawLine(
                color = color,
                start = Offset(cx, highY),
                end = Offset(cx, lowY),
                strokeWidth = 1.dp.toPx(),
            )
            drawRect(
                color = color,
                topLeft = Offset(cx - bodyWidth / 2f, minOf(openY, closeY)),
                size = Size(bodyWidth, kotlin.math.max(1.dp.toPx(), kotlin.math.abs(closeY - openY))),
            )
        }
    }
}
