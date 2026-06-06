package cz.obchodnik.ui.fng

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.domain.model.Fng
import cz.obchodnik.ui.components.ObchodnikCard
import cz.obchodnik.ui.theme.JetBrainsMono
import cz.obchodnik.ui.theme.Obchodnik
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FngScreen(
    state: FngUiState,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
    ) {
        FngTopBar()

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LinearProgressIndicator(color = c.accent, trackColor = c.surface)
            }
        } else if (state.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = state.errorMessage,
                    color = c.down,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.currentFng?.let { fng ->
                    FngGaugeCard(fng = fng)
                    FngHistoryTable(history = state.history)
                }

                if (state.history.isNotEmpty()) {
                    FngChartCard(history = state.history)
                }
            }
        }
    }
}

@Composable
private fun FngTopBar() {
    val c = Obchodnik.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Index strachu a chamtivosti",
            color = c.text,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
    }
}

@Composable
private fun FngGaugeCard(fng: Fng) {
    val c = Obchodnik.colors
    val classificationCs = translateClassification(fng.classification)
    val classificationColor = getClassificationColor(fng.value, c)

    ObchodnikCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "AKTUÁLNÍ STAV",
                color = c.text3,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            FngGauge(
                value = fng.value,
                description = "Index strachu a chamtivosti: ${fng.value} ze 100, $classificationCs",
                modifier = Modifier.size(width = 220.dp, height = 120.dp),
            )

            Text(
                text = fng.value.toString(),
                color = classificationColor,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )

            Text(
                text = classificationCs.uppercase(),
                color = classificationColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun FngGauge(value: Int, description: String, modifier: Modifier = Modifier) {
    val c = Obchodnik.colors
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        val strokeWidth = 10.dp.toPx()
        val radius = size.width / 2f - strokeWidth
        val center = Offset(size.width / 2f, size.height - 10.dp.toPx())

        val colors = listOf(
            Color(0xFFEA3943), // Extreme Fear
            Color(0xFFFF9F00), // Fear
            Color(0xFFFFD700), // Neutral
            Color(0xFF90EE90), // Greed
            Color(0xFF16C784)  // Extreme Greed
        )

        // Draw 5 segments of arc
        val segmentAngle = 180f / 5f
        for (i in 0 until 5) {
            drawArc(
                color = colors[i],
                startAngle = 180f + i * segmentAngle,
                sweepAngle = segmentAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
        }

        // Draw the Needle
        val needleValue = value.coerceIn(0, 100)
        val angleRad = Math.toRadians((180f + 180f * (needleValue / 100f)).toDouble())
        val needleLength = radius - 15.dp.toPx()
        val endPoint = Offset(
            x = center.x + (needleLength * cos(angleRad)).toFloat(),
            y = center.y + (needleLength * sin(angleRad)).toFloat()
        )

        // Draw needle line
        drawLine(
            color = c.text,
            start = center,
            end = endPoint,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Needle center hub
        drawCircle(
            color = c.text,
            radius = 6.dp.toPx(),
            center = center
        )
    }
}

@Composable
private fun FngHistoryTable(history: List<Fng>) {
    val c = Obchodnik.colors
    ObchodnikCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "HISTORICKÉ HODNOTY",
                color = c.text3,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            val today = history.getOrNull(0)
            val yesterday = history.getOrNull(1)
            val weekAgo = history.getOrNull(7) ?: history.getOrNull(6)
            val monthAgo = history.lastOrNull()

            HistoryRow(label = "Dnes", fng = today)
            HistoryRow(label = "Včera", fng = yesterday)
            HistoryRow(label = "Před týdnem", fng = weekAgo)
            HistoryRow(label = "Před měsícem", fng = monthAgo)
        }
    }
}

@Composable
private fun HistoryRow(label: String, fng: Fng?) {
    val c = Obchodnik.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = c.text2,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        if (fng != null) {
            val color = getClassificationColor(fng.value, c)
            Text(
                text = translateClassification(fng.classification),
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = fng.value.toString(),
                color = color,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        } else {
            Text(
                text = "--",
                color = c.text3,
                fontFamily = JetBrainsMono,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun FngChartCard(history: List<Fng>) {
    val c = Obchodnik.colors
    ObchodnikCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(16.dp)
    ) {
        Column {
            Text(
                text = "VÝVOJ INDEXU ZA 30 DNÍ",
                color = c.text3,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            FngHistoryChart(
                history = history.reversed(), // chronological order
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }
    }
}

@Composable
private fun FngHistoryChart(history: List<Fng>, modifier: Modifier = Modifier) {
    val c = Obchodnik.colors
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Graf vývoje indexu strachu a chamtivosti, ${history.size} hodnot"
        },
    ) {
        if (history.size < 2) return@Canvas

        val maxVal = 100f
        val minVal = 0f
        val range = maxVal - minVal

        val pointsCount = history.size
        val widthStep = size.width / (pointsCount - 1)

        val path = Path()
        val fillPath = Path()

        history.forEachIndexed { i, fng ->
            val x = i * widthStep
            val yRatio = (fng.value - minVal) / range
            val y = size.height - (yRatio * size.height)

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (i == pointsCount - 1) {
                fillPath.lineTo(x, size.height)
                fillPath.close()
            }
        }

        // Draw background area fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(c.accent.copy(alpha = 0.22f), Color.Transparent),
                startY = 0f,
                endY = size.height
            )
        )

        // Draw trend line
        drawPath(
            path = path,
            color = c.accent,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw dots on ends
        val lastX = size.width
        val lastYRatio = (history.last().value - minVal) / range
        val lastY = size.height - (lastYRatio * size.height)
        drawCircle(
            color = c.accent,
            radius = 4.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}

private fun translateClassification(cls: String): String {
    return when (cls.lowercase()) {
        "extreme fear" -> "Extrémní strach"
        "fear" -> "Strach"
        "neutral" -> "Neutrální"
        "greed" -> "Chamtivost"
        "extreme greed" -> "Extrémní chamtivost"
        else -> cls
    }
}

private fun getClassificationColor(value: Int, c: cz.obchodnik.ui.theme.ObchodnikColors): Color {
    return when {
        value < 20 -> Color(0xFFEA3943) // Extreme Fear
        value < 40 -> Color(0xFFFF9F00) // Fear
        value < 60 -> Color(0xFFFFD700) // Neutral
        value < 80 -> Color(0xFF90EE90) // Greed
        else -> Color(0xFF16C784) // Extreme Greed
    }
}
