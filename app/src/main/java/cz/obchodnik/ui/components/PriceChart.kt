package cz.obchodnik.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.core.format.MarketFormatters
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.ui.theme.JetBrainsMono
import cz.obchodnik.ui.theme.Obchodnik
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LinePriceChart(
    points: List<PricePoint>,
    color: Color,
    modifier: Modifier = Modifier,
    overlays: List<LineChartOverlay> = emptyList(),
    currency: String = "usd",
) {
    var activePoint by remember(points) { mutableStateOf<PricePoint?>(null) }
    var touchX by remember { mutableStateOf<Float?>(null) }
    var touchY by remember { mutableStateOf<Float?>(null) }
    val c = Obchodnik.colors

    BoxWithConstraints(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null && change.pressed && points.isNotEmpty()) {
                                val x = change.position.x
                                if (x >= 0 && x <= size.width) {
                                    val index = ((x / size.width) * points.lastIndex).toInt().coerceIn(0, points.lastIndex)
                                    val point = points[index]
                                    activePoint = point
                                    touchX = x

                                    val overlayPoints = overlays.flatMap { it.points }
                                    val prices = (points + overlayPoints).map { it.price }
                                    val min = prices.minOrNull() ?: 0.0
                                    val max = prices.maxOrNull() ?: 0.0
                                    val span = (max - min).takeIf { it > 0.0 } ?: 1.0
                                    val topPad = 12.dp.toPx()
                                    val chartHeight = size.height - topPad - 18.dp.toPx()
                                    touchY = topPad + chartHeight - (((point.price - min) / span).toFloat() * chartHeight)
                                }
                            } else {
                                activePoint = null
                                touchX = null
                                touchY = null
                            }
                        }
                    }
                }
        ) {
            if (points.size < 2) return@Canvas
            val overlayPoints = overlays.flatMap { it.points }
            val prices = (points + overlayPoints).map { it.price }
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
            overlays.forEach { overlay ->
                if (overlay.points.size >= 2) {
                    val overlayPath = Path()
                    overlay.points.forEach { point ->
                        val sourceIndex = points.indexOfFirst { it.timestamp == point.timestamp }
                        if (sourceIndex >= 0) {
                            val x = (sourceIndex.toFloat() / points.lastIndex.toFloat()) * size.width
                            val y = topPad + chartHeight - (((point.price - min) / span).toFloat() * chartHeight)
                            if (overlayPath.isEmpty) overlayPath.moveTo(x, y) else overlayPath.lineTo(x, y)
                        }
                    }
                    drawPath(
                        overlayPath,
                        color = overlay.color,
                        style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }

            // Draw crosshair line and active indicator circle if touching
            val tx = touchX
            val ty = touchY
            if (tx != null && ty != null) {
                drawLine(
                    color = c.text3.copy(alpha = 0.44f),
                    start = Offset(tx, 0f),
                    end = Offset(tx, size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(tx, ty))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(tx, ty))
            } else {
                val last = points.last().price
                val y = topPad + chartHeight - (((last - min) / span).toFloat() * chartHeight)
                drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(size.width, y))
            }
        }

        activePoint?.let { point ->
            val density = androidx.compose.ui.platform.LocalDensity.current
            val tx = touchX ?: 0f
            val ty = touchY ?: 0f
            val formattedPrice = MarketFormatters.price(point.price, currency)
            val formattedTime = SimpleDateFormat("d. M. HH:mm", Locale("cs", "CZ")).format(Date(point.timestamp))

            val tooltipWidth = 120.dp
            val txDp = with(density) { tx.toDp() }
            val tyDp = with(density) { ty.toDp() }

            Box(
                modifier = Modifier
                    .offset(
                        x = (txDp - tooltipWidth / 2).coerceIn(4.dp, maxWidth - tooltipWidth - 4.dp),
                        y = (tyDp - 54.dp).coerceAtLeast(4.dp)
                    )
                    .background(c.surface, RoundedCornerShape(Obchodnik.radii.chip))
                    .border(1.dp, c.borderStrong, RoundedCornerShape(Obchodnik.radii.chip))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .width(tooltipWidth)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = formattedPrice,
                        color = c.text,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Text(
                        text = formattedTime,
                        color = c.text3,
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

data class LineChartOverlay(
    val points: List<PricePoint>,
    val color: Color,
)

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
