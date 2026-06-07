package cz.obchodnik.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.core.format.MarketFormatters
import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.ui.alerts.AddAlertSheet
import cz.obchodnik.ui.components.AssetIcon
import cz.obchodnik.ui.components.CandlePriceChart
import cz.obchodnik.ui.components.Change
import cz.obchodnik.ui.components.DataApproximationDialog
import cz.obchodnik.ui.components.LineChartOverlay
import cz.obchodnik.ui.components.LinePriceChart
import cz.obchodnik.ui.components.ObchodnikCard
import cz.obchodnik.ui.theme.JetBrainsMono
import cz.obchodnik.ui.theme.Obchodnik

@Composable
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onToggleWatch: () -> Unit,
    onChartMode: (ChartMode) -> Unit,
    onRange: (ChartRange) -> Unit,
    onToggleSma7: () -> Unit,
    onToggleSma30: () -> Unit,
    onAddAlert: (String, Boolean, Double, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    var dataInfoAssetType by remember { mutableStateOf<AssetType?>(null) }
    var showAlertSheet by remember { mutableStateOf(false) }
    dataInfoAssetType?.let { assetType ->
        DataApproximationDialog(assetType = assetType, onDismiss = { dataInfoAssetType = null })
    }
    if (showAlertSheet) {
        val asset = state.asset
        if (asset != null) {
            AddAlertSheet(
                watchlist = listOf(asset),
                currency = state.currency,
                initialAssetId = asset.id,
                initialTarget = state.quote?.price,
                onDismiss = { showAlertSheet = false },
                onConfirm = { assetId, above, target, repeating ->
                    onAddAlert(assetId, above, target, repeating)
                    showAlertSheet = false
                },
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        DetailTopBar(
            title = state.asset?.symbol ?: "Detail",
            subtitle = state.asset?.name,
            inWatchlist = state.inWatchlist,
            onBack = onBack,
            onToggleWatch = onToggleWatch,
            onAddAlert = { showAlertSheet = true },
        )
        if (state.isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.accent, trackColor = c.surface)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                HeaderSection(state = state, modifier = Modifier.padding(16.dp))
            }
            item {
                ChartModeToggle(
                    selected = state.chartMode,
                    onSelected = onChartMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            if (state.chartMode == ChartMode.LINE) {
                item {
                    IndicatorToggleRow(
                        state = state,
                        onToggleSma7 = onToggleSma7,
                        onToggleSma30 = onToggleSma30,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            item {
                val positive = (state.quote?.change24hPct ?: 0.0) >= 0.0
                val lineColor = if (positive) c.up else c.down
                val overlays = buildList {
                    if (state.showSma7) {
                        add(LineChartOverlay(points = state.sma7Points, color = c.accent))
                    }
                    if (state.showSma30) {
                        add(LineChartOverlay(points = state.sma30Points, color = c.text2))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(226.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .semantics {
                            contentDescription = "Cenový graf aktiva " +
                                (state.asset?.name ?: "") +
                                ", změna za 24 hodin " +
                                MarketFormatters.percent(state.quote?.change24hPct)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.chartMode == ChartMode.LINE) {
                        LinePriceChart(
                            points = state.linePoints,
                            color = lineColor,
                            overlays = overlays,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        CandlePriceChart(
                            candles = state.candles,
                            upColor = c.up,
                            downColor = c.down,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    if (!state.isLoading && state.linePoints.isEmpty() && state.candles.isEmpty()) {
                        Text(text = "Graf zatím není dostupný", color = c.text3, fontSize = 13.sp)
                    }
                }
            }
            item {
                RangeTabs(
                    selected = state.range,
                    onSelected = onRange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage,
                        color = c.down,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            item {
                StatsGrid(state = state, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }
            item {
                DataInfoFooter(
                    state = state,
                    onOpenDataInfo = { dataInfoAssetType = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                ActionButtons(
                    inWatchlist = state.inWatchlist,
                    onToggleWatch = onToggleWatch,
                    onAddAlert = { showAlertSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun IndicatorToggleRow(
    state: DetailUiState,
    onToggleSma7: () -> Unit,
    onToggleSma30: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IndicatorChip(
            label = "SMA 7",
            active = state.showSma7,
            enabled = state.sma7Points.isNotEmpty(),
            onClick = onToggleSma7,
        )
        IndicatorChip(
            label = "SMA 30",
            active = state.showSma30,
            enabled = state.sma30Points.isNotEmpty(),
            onClick = onToggleSma30,
        )
    }
}

@Composable
private fun IndicatorChip(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = Obchodnik.colors
    Text(
        text = label,
        color = when {
            active -> c.onAccent
            enabled -> c.text2
            else -> c.text3.copy(alpha = 0.45f)
        },
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier
            .background(if (active) c.accent else c.surface, RoundedCornerShape(Obchodnik.radii.chip))
            .border(BorderStroke(1.dp, if (active) c.accent else c.border), RoundedCornerShape(Obchodnik.radii.chip))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun DetailTopBar(
    title: String,
    subtitle: String?,
    inWatchlist: Boolean,
    onBack: () -> Unit,
    onToggleWatch: () -> Unit,
    onAddAlert: () -> Unit,
) {
    val c = Obchodnik.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .padding(start = 4.dp, end = 4.dp, top = 48.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět", tint = c.text)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            if (subtitle != null) {
                Text(text = subtitle, color = c.text3, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onToggleWatch) {
            Icon(
                imageVector = if (inWatchlist) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = "Sledovat",
                tint = if (inWatchlist) c.accent else c.text2,
            )
        }
        IconButton(onClick = onAddAlert) {
            Icon(Icons.Rounded.Notifications, contentDescription = "Alert", tint = c.text2)
        }
    }
}

@Composable
private fun HeaderSection(state: DetailUiState, modifier: Modifier = Modifier) {
    val c = Obchodnik.colors
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AssetIcon(symbol = state.asset?.symbol.orEmpty(), colorHex = state.asset?.colorHex, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = MarketFormatters.price(state.quote?.price, state.currency),
                color = c.text,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                maxLines = 1,
            )
            Change(value = state.quote?.change24hPct, chip = true, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable
private fun ChartModeToggle(
    selected: ChartMode,
    onSelected: (ChartMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    Row(modifier = modifier, horizontalArrangement = Arrangement.End) {
        Row(
            modifier = Modifier
                .background(c.surface, RoundedCornerShape(Obchodnik.radii.chip))
                .border(1.dp, c.border, RoundedCornerShape(Obchodnik.radii.chip))
                .padding(3.dp),
        ) {
            ChartMode.entries.forEach { mode ->
                val active = selected == mode
                Text(
                    text = mode.label,
                    color = if (active) c.onAccent else c.text3,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(if (active) c.accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(Obchodnik.radii.chip))
                        .clickable { onSelected(mode) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun RangeTabs(
    selected: ChartRange,
    onSelected: (ChartRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ChartRange.entries.forEach { range ->
            val active = selected == range
            Text(
                text = range.label,
                color = if (active) c.text else c.text3,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier
                    .weight(1f)
                    .background(if (active) c.surface2 else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(Obchodnik.radii.chip))
                    .border(BorderStroke(1.dp, if (active) c.borderStrong else androidx.compose.ui.graphics.Color.Transparent), RoundedCornerShape(Obchodnik.radii.chip))
                    .clickable { onSelected(range) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun DataInfoFooter(
    state: DetailUiState,
    onOpenDataInfo: (AssetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    val asset = state.asset
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val updated = MarketFormatters.time(state.quote?.updatedAt ?: 0L)
        val source = asset?.source?.let { MarketFormatters.sourceLabel(it) } ?: "—"
        Text(
            text = "Aktualizováno $updated · Zdroj: $source",
            color = c.text3,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
        )
        if (state.noticeMessage != null) {
            Text(
                text = state.noticeMessage,
                color = c.text2,
                fontSize = 11.sp,
            )
        }
        when (asset?.type) {
            AssetType.INDEX -> Text(
                text = "Hodnota přibližně přes ETF zástupce. Více o datech",
                color = c.text2,
                fontSize = 11.sp,
                modifier = Modifier.clickable { onOpenDataInfo(AssetType.INDEX) },
            )
            AssetType.COMMODITY -> Text(
                text = "Komoditní data se aktualizují přibližně jednou denně. Více o datech",
                color = c.text2,
                fontSize = 11.sp,
                modifier = Modifier.clickable { onOpenDataInfo(AssetType.COMMODITY) },
            )
            else -> {}
        }
    }
}

@Composable
private fun StatsGrid(state: DetailUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("24h max", MarketFormatters.price(state.quote?.high24h, state.currency), Modifier.weight(1f))
            StatTile("24h min", MarketFormatters.price(state.quote?.low24h, state.currency), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Změna 7 d", MarketFormatters.percent(state.quote?.change7dPct), Modifier.weight(1f))
            StatTile("Změna 30 d", MarketFormatters.percent(state.quote?.change30dPct), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Tržní kap.", compact(state.quote?.marketCap), Modifier.weight(1f))
            StatTile("Objem 24h", compact(state.quote?.volume24h), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Obchodnik.colors
    ObchodnikCard(modifier = modifier, padding = PaddingValues(horizontal = 13.dp, vertical = 11.dp)) {
        Column {
            Text(text = label, color = c.text3, fontSize = 11.sp)
            Text(text = value, color = c.text, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun ActionButtons(
    inWatchlist: Boolean,
    onToggleWatch: () -> Unit,
    onAddAlert: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(Obchodnik.radii.radius))
                .border(1.dp, c.borderStrong, RoundedCornerShape(Obchodnik.radii.radius))
                .clickable { onAddAlert() }
                .padding(13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = c.text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "Alert", color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(Obchodnik.radii.radius))
                .background(c.accent, RoundedCornerShape(Obchodnik.radii.radius))
                .clickable { onToggleWatch() }
                .padding(13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (inWatchlist) Icons.Rounded.Check else Icons.Rounded.Add, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = if (inWatchlist) "Ve watchlistu" else "Sledovat", color = c.onAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

private val ChartRange.label: String
    get() = when (this) {
        ChartRange.D1 -> "1D"
        ChartRange.W1 -> "1T"
        ChartRange.M1 -> "1M"
        ChartRange.Y1 -> "1R"
        ChartRange.ALL -> "VŠE"
    }

private fun compact(value: Double?): String {
    if (value == null) return "—"
    return when {
        value >= 1_000_000_000_000 -> "${(value / 1_000_000_000_000).format1()} bil."
        value >= 1_000_000_000 -> "${(value / 1_000_000_000).format1()} mld."
        value >= 1_000_000 -> "${(value / 1_000_000).format1()} mil."
        value >= 1_000 -> "${(value / 1_000).format1()} tis."
        else -> value.format1()
    }
}

private fun Double.format1(): String =
    java.text.NumberFormat.getNumberInstance(java.util.Locale("cs", "CZ")).apply {
        maximumFractionDigits = 1
    }.format(this)
