package cz.obchodnik.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import cz.obchodnik.MainActivity
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.core.format.MarketFormatters
import cz.obchodnik.data.local.entity.AssetEntity
import cz.obchodnik.data.local.entity.QuoteEntity
import cz.obchodnik.data.local.toDomain
import cz.obchodnik.domain.model.Quote
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ObchodnikWidgetKeys {
    val assets = stringPreferencesKey("assets")
    val showFng = booleanPreferencesKey("show_fng")
}

class ObchodnikWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    companion object {
        private val SMALL_SQUARE = DpSize(110.dp, 110.dp)
        private val HORIZONTAL_RECT = DpSize(250.dp, 110.dp)
        private val BIG_SQUARE = DpSize(250.dp, 250.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL_SQUARE, HORIZONTAL_RECT, BIG_SQUARE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ObchodnikApp
        val assetDao = app.container.database.assetDao()
        val quoteDao = app.container.database.quoteDao()
        val settingsStore = app.container.settingsRepository
        val json = app.container.json

        // Load preferences (suspend)
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val configAssetsStr = prefs[ObchodnikWidgetKeys.assets] ?: "cg:bitcoin"
        val showFng = prefs[ObchodnikWidgetKeys.showFng] ?: true

        // Read settings from DataStore (suspend)
        val settings = runCatching { settingsStore.settings.first() }.getOrNull()
        val currency = settings?.currency ?: "usd"
        val themeName = settings?.theme ?: "terminal"
        val accentName = settings?.accent ?: "blue"

        // Load assets and quotes (suspend)
        val selectedAssetIds = configAssetsStr.split(",").filter { it.isNotBlank() }
        val dbAssets = assetDao.watchlistAssets().associateBy { it.id }
        
        // Map selected assets or fallback to default
        val assets = selectedAssetIds.mapNotNull { dbAssets[it] }
            .takeIf { it.isNotEmpty() }
            ?: dbAssets.values.take(5).toList()

        val quotes = quoteDao.quotesForAssets(assets.map { it.id }, currency)
            .map { it.toDomain(json) }
            .associateBy { it.assetId }

        provideContent {
            val size = LocalSize.current

            // Theme colors
            val surfaceColor = when (themeName) {
                "aurora" -> Color(0xE615172A)
                "mono" -> Color(0xE6121212)
                else -> Color(0xE60E1218) // terminal
            }
            val accentColor = when (accentName) {
                "green" -> Color(0xFF16C784)
                "orange" -> Color(0xFFF7931A)
                "purple" -> Color(0xFFA78BFA)
                else -> Color(0xFF3B82F6) // blue
            }

            val layoutType = when {
                size.width >= 250.dp && size.height >= 250.dp -> WidgetLayoutType.LARGE
                size.width >= 250.dp -> WidgetLayoutType.MEDIUM
                else -> WidgetLayoutType.SMALL
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(surfaceColor)
                    .padding(14.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                when (layoutType) {
                    WidgetLayoutType.SMALL -> {
                        val asset = assets.firstOrNull()
                        val quote = asset?.let { quotes[it.id] }
                        SmallWidgetLayout(
                            asset = asset,
                            quote = quote,
                            currency = currency,
                            accentColor = accentColor
                        )
                    }
                    WidgetLayoutType.MEDIUM -> {
                        MediumWidgetLayout(
                            assets = assets.take(3),
                            quotes = quotes,
                            currency = currency,
                            showFng = showFng,
                            accentColor = accentColor
                        )
                    }
                    WidgetLayoutType.LARGE -> {
                        LargeWidgetLayout(
                            assets = assets.take(5),
                            quotes = quotes,
                            currency = currency,
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }
}

private enum class WidgetLayoutType {
    SMALL, MEDIUM, LARGE
}

@Composable
private fun WidgetHeader(accentColor: Color, rightContent: @Composable () -> Unit = {}) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(20.dp),
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(14.dp)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = GlanceModifier.size(5.dp).background(Color.White)) {}
            }
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "Obchodník",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        rightContent()
    }
}

@Composable
private fun SmallWidgetLayout(
    asset: AssetEntity?,
    quote: Quote?,
    currency: String,
    accentColor: Color
) {
    if (asset == null) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Žádná data", style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp))
        }
        return
    }

    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val change = quote?.change24hPct ?: 0.0
    val up = change >= 0.0
    val changeColor = if (up) Color(0xFF16C784) else Color(0xFFEA3943)

    Column(modifier = GlanceModifier.fillMaxSize()) {
        WidgetHeader(accentColor)
        Spacer(modifier = GlanceModifier.height(4.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = asset.symbol,
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = asset.name,
                style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 10.sp)
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = MarketFormatters.price(quote?.price, currency),
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            text = "${if (change >= 0) "+" else ""}${MarketFormatters.percent(change)} · 24h",
            style = TextStyle(color = ColorProvider(changeColor), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        val sparkline = quote?.sparkline7d.orEmpty()
        if (sparkline.size >= 2) {
            val bitmap = drawSparkline(sparkline, up, 120, 28, density)
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxWidth().height(28.dp)
            )
        } else {
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun MediumWidgetLayout(
    assets: List<AssetEntity>,
    quotes: Map<String, Quote>,
    currency: String,
    showFng: Boolean,
    accentColor: Color
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    
    Column(modifier = GlanceModifier.fillMaxSize()) {
        WidgetHeader(accentColor, rightContent = {
            if (showFng) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Fear & Greed",
                        style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 9.sp)
                    )
                    Spacer(modifier = GlanceModifier.width(5.dp))
                    val fngBitmap = drawFngMini(72, 24, density)
                    Image(
                        provider = ImageProvider(fngBitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.size(24.dp)
                    )
                }
            }
        })
        Spacer(modifier = GlanceModifier.height(6.dp))
        Column(
            modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            assets.forEachIndexed { index, asset ->
                if (index > 0) Spacer(modifier = GlanceModifier.height(6.dp))
                WidgetAssetRow(
                    asset = asset,
                    quote = quotes[asset.id],
                    currency = currency,
                    density = density,
                    showSpark = true
                )
            }
        }
    }
}

@Composable
private fun LargeWidgetLayout(
    assets: List<AssetEntity>,
    quotes: Map<String, Quote>,
    currency: String,
    accentColor: Color
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    Column(modifier = GlanceModifier.fillMaxSize()) {
        WidgetHeader(accentColor, rightContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Fear & Greed",
                        style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 8.sp)
                    )
                    Text(
                        text = "Chamtivost",
                        style = TextStyle(color = ColorProvider(Color(0xFF84CC16)), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = GlanceModifier.width(6.dp))
                val fngBitmap = drawFngMini(72, 28, density)
                Image(
                    provider = ImageProvider(fngBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.size(28.dp)
                )
            }
        })
        Spacer(modifier = GlanceModifier.height(6.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            assets.forEachIndexed { index, asset ->
                if (index > 0) {
                    Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(Color(0x12FFFFFF))) {}
                }
                WidgetAssetRow(
                    asset = asset,
                    quote = quotes[asset.id],
                    currency = currency,
                    density = density,
                    showSpark = true
                )
            }
        }
        Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(Color(0x12FFFFFF))) {}
        Spacer(modifier = GlanceModifier.height(6.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aktualizováno $timeStr",
                style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 9.sp)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "Otevřít aplikaci →",
                style = TextStyle(color = ColorProvider(accentColor), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun WidgetAssetRow(
    asset: AssetEntity,
    quote: Quote?,
    currency: String,
    density: Float,
    showSpark: Boolean
) {
    val change = quote?.change24hPct ?: 0.0
    val up = change >= 0.0
    val changeColor = if (up) Color(0xFF16C784) else Color(0xFFEA3943)
    val color = runCatching { Color(android.graphics.Color.parseColor(asset.colorHex ?: "#3B82F6")) }.getOrDefault(Color(0xFF3B82F6))

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo monogram
        val monogram = if (asset.symbol.length > 4) asset.symbol.substring(0, 3) else asset.symbol.substring(0, 1)
        Box(
            modifier = GlanceModifier
                .size(24.dp)
                .background(color.copy(alpha = 0.15f))
                .padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monogram,
                style = TextStyle(color = ColorProvider(color), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.width(48.dp)) {
            Text(
                text = asset.symbol,
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }
        if (showSpark) {
            val sparkline = quote?.sparkline7d.orEmpty()
            if (sparkline.size >= 2) {
                val bitmap = drawSparkline(sparkline, up, 48, 18, density)
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.defaultWeight().height(18.dp).padding(horizontal = 6.dp)
                )
            } else {
                Spacer(modifier = GlanceModifier.defaultWeight())
            }
        } else {
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = MarketFormatters.price(quote?.price, currency),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${if (change >= 0) "+" else ""}${MarketFormatters.percent(change)}",
                style = TextStyle(color = ColorProvider(changeColor), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            )
        }
    }
}

private fun drawSparkline(
    values: List<Double>,
    isUp: Boolean,
    widthDp: Int,
    heightDp: Int,
    density: Float
): Bitmap {
    val widthPx = (widthDp * density).toInt().coerceAtLeast(1)
    val heightPx = (heightDp * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    if (values.isEmpty()) return bitmap

    val paint = Paint().apply {
        color = if (isUp) 0xFF16C784.toInt() else 0xFFEA3943.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * density
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val minVal = values.minOrNull() ?: 0.0
    val maxVal = values.maxOrNull() ?: 0.0
    val range = maxVal - minVal

    val path = android.graphics.Path()
    val stepX = widthPx.toFloat() / (values.size - 1).coerceAtLeast(1)

    for (i in values.indices) {
        val x = i * stepX
        val normY = if (range > 0) (values[i] - minVal) / range else 0.5
        val y = heightPx.toFloat() - (normY * heightPx.toFloat()).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    canvas.drawPath(path, paint)
    return bitmap
}

private fun drawFngMini(value: Int, sizeDp: Int, density: Float): Bitmap {
    val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val strokeWidth = 2.5f * density
    val center = sizePx / 2f
    val radius = (sizePx / 2f) - strokeWidth

    val bgPaint = Paint().apply {
        color = 0x1AFFFFFF.toInt()
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        isAntiAlias = true
    }
    canvas.drawCircle(center, center, radius, bgPaint)

    val fngColor = when {
        value < 25 -> 0xFFEF4444.toInt()
        value < 45 -> 0xFFF59E0B.toInt()
        value < 55 -> 0xFFEAB308.toInt()
        value < 75 -> 0xFF84CC16.toInt()
        else -> 0xFF22C55E.toInt()
    }

    val fgPaint = Paint().apply {
        color = fngColor
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    val rect = RectF(center - radius, center - radius, center + radius, center + radius)
    val sweepAngle = 360f * (value / 100f)
    canvas.drawArc(rect, -90f, sweepAngle, false, fgPaint)

    val textPaint = Paint().apply {
        color = fngColor
        textSize = sizePx * 0.35f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.MONOSPACE
        isFakeBoldText = true
    }

    val yPos = center - (textPaint.descent() + textPaint.ascent()) / 2
    canvas.drawText(value.toString(), center, yPos, textPaint)

    return bitmap
}
