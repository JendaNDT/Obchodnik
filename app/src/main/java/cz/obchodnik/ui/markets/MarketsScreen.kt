package cz.obchodnik.ui.markets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.core.format.MarketFormatters
import cz.obchodnik.domain.AssetType
import cz.obchodnik.ui.components.AssetIcon
import cz.obchodnik.ui.components.Change
import cz.obchodnik.ui.components.DataApproximationDialog
import cz.obchodnik.ui.components.ObchodnikCard
import cz.obchodnik.ui.components.SkeletonBlock
import cz.obchodnik.ui.components.Sparkline
import cz.obchodnik.ui.theme.JetBrainsMono
import cz.obchodnik.ui.theme.Obchodnik

@Composable
fun MarketsScreen(
    state: MarketsUiState,
    onCategorySelected: (MarketCategory) -> Unit,
    onQueryChanged: (String) -> Unit,
    onSortModeSelected: (MarketSortMode) -> Unit,
    onQuickViewSelected: (MarketQuickView) -> Unit,
    onApplySavedView: (SavedMarketView) -> Unit,
    onSaveCurrentView: (String) -> Unit,
    onDeleteSavedView: (String) -> Unit,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onOpenAsset: (String) -> Unit,
    onMoveAsset: (String, Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAlerts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var pullDistance by remember { mutableFloatStateOf(0f) }
    var dataInfoAssetType by remember { mutableStateOf<AssetType?>(null) }
    var showSaveViewDialog by remember { mutableStateOf(false) }
    val pullThreshold = 92.dp
    val c = Obchodnik.colors
    dataInfoAssetType?.let { assetType ->
        DataApproximationDialog(assetType = assetType, onDismiss = { dataInfoAssetType = null })
    }
    if (showSaveViewDialog) {
        SaveViewDialog(
            onConfirm = { name ->
                onSaveCurrentView(name)
                showSaveViewDialog = false
            },
            onDismiss = { showSaveViewDialog = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        MarketsTopBar(
            onRefresh = onRefresh,
            onSearch = onSearch,
            onOpenSettings = onOpenSettings,
            onOpenAlerts = onOpenAlerts
        )
        if (state.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = c.accent,
                trackColor = c.surface,
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.isRefreshing, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            if (listState.isAtTop() && dragAmount > 0 && !state.isRefreshing) {
                                pullDistance += dragAmount
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            if (pullDistance > pullThreshold.toPx()) onRefresh()
                            pullDistance = 0f
                        },
                        onDragCancel = { pullDistance = 0f },
                    )
                },
            state = listState,
            contentPadding = PaddingValues(bottom = 22.dp),
        ) {
            item {
                FearGreedCard(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                )
            }
            item {
                CategoryTabs(
                    selected = state.selectedCategory,
                    onSelected = onCategorySelected,
                )
            }
            item {
                QuickViewChips(
                    active = state.activeQuickView,
                    onSelected = onQuickViewSelected,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            item {
                SavedViewsRow(
                    views = state.savedViews,
                    onApply = onApplySavedView,
                    onDelete = onDeleteSavedView,
                    onSaveCurrent = { showSaveViewDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            item {
                WatchlistControls(
                    query = state.query,
                    selectedSortMode = state.sortMode,
                    onQueryChanged = onQueryChanged,
                    onSortModeSelected = onSortModeSelected,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            if (state.errorMessage != null) {
                item {
                    ErrorStrip(
                        message = state.errorMessage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            if (state.noticeMessage != null) {
                item {
                    NoticeStrip(
                        message = state.noticeMessage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            if (state.isLoading) {
                items(6) {
                    MarketSkeletonRow()
                }
            } else if (state.assets.isEmpty()) {
                item {
                    EmptyWatchlist(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 54.dp),
                    )
                }
            } else {
                itemsIndexed(
                    items = state.assets,
                    key = { _, row -> row.asset.id },
                ) { index, row ->
                    MarketAssetRow(
                        row = row,
                        currency = state.currency,
                        showReorder = state.selectedCategory == MarketCategory.ALL &&
                            state.query.isBlank() &&
                            state.sortMode == MarketSortMode.MANUAL,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.assets.lastIndex,
                        onMoveUp = { onMoveAsset(row.asset.id, -1) },
                        onMoveDown = { onMoveAsset(row.asset.id, 1) },
                        onOpenDataInfo = { dataInfoAssetType = row.asset.type },
                        onClick = { onOpenAsset(row.asset.id) },
                    )
                }
            }
            item {
                AddAssetCta(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    onClick = onSearch,
                )
            }
        }
    }
}

@Composable
private fun QuickViewChips(
    active: MarketQuickView?,
    onSelected: (MarketQuickView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MarketQuickView.entries.forEach { view ->
            val selected = active == view
            Text(
                text = view.label,
                color = if (selected) c.onAccent else c.text2,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selected) c.accent else c.surface,
                        shape = RoundedCornerShape(Obchodnik.radii.chip),
                    )
                    .border(
                        BorderStroke(1.dp, if (selected) c.accent else c.border),
                        RoundedCornerShape(Obchodnik.radii.chip),
                    )
                    .clickable { onSelected(view) }
                    .padding(vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun WatchlistControls(
    query: String,
    selectedSortMode: MarketSortMode,
    onQueryChanged: (String) -> Unit,
    onSortModeSelected: (MarketSortMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(text = "Filtrovat watchlist", color = c.text3, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = c.text3,
                    modifier = Modifier.size(18.dp),
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = c.text,
                unfocusedTextColor = c.text,
                focusedBorderColor = c.borderStrong,
                unfocusedBorderColor = c.border,
                cursorColor = c.accent,
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            MarketSortMode.entries.forEach { mode ->
                val active = selectedSortMode == mode
                Text(
                    text = mode.label,
                    color = if (active) c.onAccent else c.text2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (active) c.accent else c.surface,
                            shape = RoundedCornerShape(Obchodnik.radii.chip),
                        )
                        .border(
                            BorderStroke(1.dp, if (active) c.accent else c.border),
                            RoundedCornerShape(Obchodnik.radii.chip),
                        )
                        .clickable { onSortModeSelected(mode) }
                        .padding(vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun MarketsTopBar(
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAlerts: () -> Unit,
) {
    val c = Obchodnik.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .padding(start = 16.dp, end = 4.dp, top = 48.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Trh",
            color = c.text,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearch) {
            Icon(Icons.Rounded.Search, contentDescription = "Hledat", tint = c.text2)
        }
        IconButton(onClick = onOpenAlerts) {
            Box {
                Icon(Icons.Rounded.Notifications, contentDescription = "Alerty", tint = c.text2)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp)
                        .background(c.accent, CircleShape)
                        .border(1.dp, c.bg, CircleShape),
                )
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Obnovit", tint = c.text2)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Nastavení", tint = c.text2)
        }
    }
}

@Composable
private fun FearGreedCard(modifier: Modifier = Modifier) {
    val c = Obchodnik.colors
    ObchodnikCard(modifier = modifier, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniGauge(value = 72, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "INDEX STRACHU A CHAMTIVOSTI",
                    color = c.text3,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                )
                Text(
                    text = "Chamtivost",
                    color = c.up,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Text(
                text = "72",
                color = c.up,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
    }
}

@Composable
private fun MiniGauge(value: Int, modifier: Modifier = Modifier) {
    val c = Obchodnik.colors
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        val radius = size.minDimension / 2f - 4.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = c.borderStrong, radius = radius, center = center, style = stroke)
        drawArc(
            color = c.up,
            startAngle = -90f,
            sweepAngle = 360f * (value / 100f),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = stroke,
        )
    }
}

@Composable
private fun CategoryTabs(
    selected: MarketCategory,
    onSelected: (MarketCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MarketCategory.entries.forEach { category ->
            val active = selected == category
            val c = Obchodnik.colors
            Text(
                text = category.label,
                color = if (active) c.onAccent else c.text2,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .background(
                        color = if (active) c.accent else c.surface,
                        shape = RoundedCornerShape(Obchodnik.radii.chip),
                    )
                    .border(
                        BorderStroke(1.dp, if (active) c.accent else c.border),
                        RoundedCornerShape(Obchodnik.radii.chip),
                    )
                    .clickable { onSelected(category) }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MarketAssetRow(
    row: MarketAssetUi,
    currency: String,
    showReorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOpenDataInfo: () -> Unit,
    onClick: () -> Unit,
) {
    val c = Obchodnik.colors
    val quote = row.quote
    val up = (quote?.change24hPct ?: 0.0) >= 0.0
    val lineColor = if (up) c.up else c.down
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetIcon(symbol = row.asset.symbol, colorHex = row.asset.colorHex)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.width(92.dp)) {
            Text(
                text = row.asset.symbol,
                color = c.text,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                text = row.asset.name,
                color = c.text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
            )
            val dataBadge = when (row.asset.type) {
                AssetType.INDEX -> "≈ ETF"
                AssetType.COMMODITY -> "denní data"
                else -> null
            }
            if (dataBadge != null) {
                Text(
                    text = dataBadge,
                    color = c.text3,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .background(c.surface2, RoundedCornerShape(Obchodnik.radii.chip))
                        .border(BorderStroke(1.dp, c.border), RoundedCornerShape(Obchodnik.radii.chip))
                        .clickable { onOpenDataInfo() }
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
                .padding(horizontal = 8.dp)
                .semantics { contentDescription = "Graf vývoje ceny za 7 dní" },
            contentAlignment = Alignment.Center,
        ) {
            if ((quote?.sparkline7d?.size ?: 0) >= 2) {
                Sparkline(
                    values = quote?.sparkline7d.orEmpty(),
                    color = lineColor,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                SkeletonBlock(Modifier.fillMaxWidth(0.72f).height(8.dp))
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(96.dp)) {
            Text(
                text = MarketFormatters.price(quote?.price, currency),
                color = c.text,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
            )
            Change(value = quote?.change24hPct, modifier = Modifier.padding(top = 3.dp))
        }
        if (showReorder) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(30.dp)) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Posunout nahoru",
                        tint = if (canMoveUp) c.text2 else c.text3.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Posunout dolů",
                        tint = if (canMoveDown) c.text2 else c.text3.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(Modifier.size(38.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBlock(Modifier.width(62.dp).height(13.dp))
            Spacer(Modifier.height(7.dp))
            SkeletonBlock(Modifier.width(104.dp).height(11.dp))
        }
        SkeletonBlock(Modifier.width(78.dp).height(13.dp))
    }
}

@Composable
private fun NoticeStrip(message: String, modifier: Modifier = Modifier) {
    val c = Obchodnik.colors
    Text(
        text = message,
        color = c.text2,
        fontSize = 12.sp,
        modifier = modifier
            .background(c.accent.copy(alpha = 0.10f), RoundedCornerShape(Obchodnik.radii.radiusSm))
            .border(1.dp, c.accent.copy(alpha = 0.24f), RoundedCornerShape(Obchodnik.radii.radiusSm))
            .padding(12.dp),
    )
}

@Composable
private fun ErrorStrip(message: String, modifier: Modifier = Modifier) {
    val c = Obchodnik.colors
    Text(
        text = message,
        color = c.down,
        fontSize = 12.sp,
        modifier = modifier
            .background(c.down.copy(alpha = 0.10f), RoundedCornerShape(Obchodnik.radii.radiusSm))
            .border(1.dp, c.down.copy(alpha = 0.24f), RoundedCornerShape(Obchodnik.radii.radiusSm))
            .padding(12.dp),
    )
}

@Composable
private fun EmptyWatchlist(modifier: Modifier = Modifier) {
    val c = Obchodnik.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AssetIcon(symbol = "O", colorHex = null, size = 64.dp)
        Spacer(Modifier.height(14.dp))
        Text(text = "Prázdný watchlist", color = c.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(
            text = "Přidej si aktiva, která chceš sledovat.",
            color = c.text3,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AddAssetCta(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = Obchodnik.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, c.borderStrong), RoundedCornerShape(Obchodnik.radii.radius))
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = c.text2, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = "Přidat aktivum do watchlistu", color = c.text2, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun SavedViewsRow(
    views: List<SavedMarketView>,
    onApply: (SavedMarketView) -> Unit,
    onDelete: (String) -> Unit,
    onSaveCurrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .background(c.surface, RoundedCornerShape(Obchodnik.radii.chip))
                .border(BorderStroke(1.dp, c.border), RoundedCornerShape(Obchodnik.radii.chip))
                .clickable { onSaveCurrent() }
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = c.text2,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Uložit pohled",
                color = c.text2,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
        views.forEach { view ->
            Row(
                modifier = Modifier
                    .background(c.surface, RoundedCornerShape(Obchodnik.radii.chip))
                    .border(BorderStroke(1.dp, c.border), RoundedCornerShape(Obchodnik.radii.chip))
                    .padding(start = 10.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = view.name,
                    color = c.text2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .clickable { onApply(view) }
                        .padding(vertical = 7.dp),
                )
                IconButton(
                    onClick = { onDelete(view.id) },
                    modifier = Modifier.size(22.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Smazat pohled " + view.name,
                        tint = c.text3,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveViewDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Obchodnik.colors
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Text(text = "Uložit pohled", color = c.text, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(text = "Název pohledu", color = c.text3) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = c.text,
                    unfocusedTextColor = c.text,
                    focusedBorderColor = c.borderStrong,
                    unfocusedBorderColor = c.border,
                    cursorColor = c.accent,
                    focusedContainerColor = c.surface,
                    unfocusedContainerColor = c.surface,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(text = "Uložit", color = if (name.isNotBlank()) c.accent else c.text3)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Zrušit", color = c.text2)
            }
        },
    )
}

private fun LazyListState.isAtTop(): Boolean =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
