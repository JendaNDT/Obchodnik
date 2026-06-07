package cz.obchodnik.ui.portfolio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.core.format.MarketFormatters
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Holding
import cz.obchodnik.ui.components.AssetIcon
import cz.obchodnik.ui.components.Change
import cz.obchodnik.ui.components.ObchodnikCard
import cz.obchodnik.ui.theme.JetBrainsMono
import cz.obchodnik.ui.theme.Obchodnik

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    state: PortfolioUiState,
    onAddPosition: (String, Double, Double) -> Unit,
    onUpdatePosition: (Holding, Double, Double) -> Unit,
    onDeletePosition: (Holding) -> Unit,
    onExportCsv: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    var showSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PortfolioItem?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(onExportCsv) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
    ) {
        PortfolioTopBar(
            canExport = state.items.isNotEmpty(),
            onExportClick = { exportLauncher.launch("obchodnik-portfolio.csv") },
            onAddClick = {
                editingItem = null
                showSheet = true
            },
        )

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LinearProgressIndicator(color = c.accent, trackColor = c.surface)
            }
        } else if (state.items.isEmpty()) {
            EmptyPortfolio(onAddClick = { showSheet = true })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SummaryCard(state = state)
                }

                item {
                    Text(
                        text = "AKTIVNÍ POZICE",
                        color = c.text3,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                items(
                    items = state.items,
                    key = { it.holding.id }
                ) { item ->
                    HoldingRow(
                        item = item,
                        currency = state.currency,
                        onEdit = {
                            editingItem = item
                            showSheet = true
                        },
                        onDelete = { onDeletePosition(item.holding) }
                    )
                }
            }
        }
    }

    if (showSheet) {
        PositionSheet(
            watchlist = state.allWatchlistAssets,
            currency = state.currency,
            editingItem = editingItem,
            onDismiss = { showSheet = false },
            onConfirm = { assetId, qty, price ->
                val current = editingItem?.holding
                if (current == null) {
                    onAddPosition(assetId, qty, price)
                } else {
                    onUpdatePosition(current.copy(assetId = assetId), qty, price)
                }
                showSheet = false
                editingItem = null
            }
        )
    }
}

@Composable
private fun PortfolioTopBar(
    canExport: Boolean,
    onExportClick: () -> Unit,
    onAddClick: () -> Unit,
) {
    val c = Obchodnik.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .padding(start = 16.dp, end = 4.dp, top = 48.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Portfolio",
            color = c.text,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onExportClick, enabled = canExport) {
            Icon(
                imageVector = Icons.Rounded.FileDownload,
                contentDescription = "Exportovat portfolio do CSV",
                tint = if (canExport) c.text else c.text3.copy(alpha = 0.45f),
            )
        }
        IconButton(onClick = onAddClick) {
            Icon(Icons.Rounded.Add, contentDescription = "Přidat pozici", tint = c.text)
        }
    }
}

@Composable
private fun SummaryCard(state: PortfolioUiState) {
    val c = Obchodnik.colors
    val plUp = state.totalPL >= 0.0
    val plColor = if (plUp) c.up else c.down

    ObchodnikCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(16.dp)
    ) {
        Column {
            Text(
                text = "CELKOVÁ HODNOTA",
                color = c.text3,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
            Text(
                text = MarketFormatters.price(state.totalValue, state.currency),
                color = c.text,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Celkový P/L: ",
                    color = c.text2,
                    fontSize = 13.sp
                )
                Text(
                    text = (if (state.totalPL >= 0.0) "+" else "") + MarketFormatters.price(state.totalPL, state.currency),
                    color = plColor,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.width(6.dp))
                Change(value = state.totalPLPct)
            }

            Spacer(Modifier.height(16.dp))

            // Allocation Bar
            AllocationProgressBar(items = state.items)
        }
    }
}

@Composable
private fun AllocationProgressBar(items: List<PortfolioItem>) {
    val totalVal = items.sumOf { it.value }
    if (totalVal <= 0.0) return

    val c = Obchodnik.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(c.borderStrong)
    ) {
        items.forEach { item ->
            val ratio = (item.value / totalVal).toFloat()
            if (ratio > 0.01f) {
                val hexColor = item.asset.colorHex ?: "#3b82f6"
                val parsedColor = runCatching { Color(android.graphics.Color.parseColor(hexColor)) }.getOrDefault(c.accent)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .weight(ratio)
                        .height(6.dp)
                        .background(parsedColor)
                )
            }
        }
    }
}

@Composable
private fun HoldingRow(
    item: PortfolioItem,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = Obchodnik.colors
    val plColor = if (item.plValue >= 0.0) c.up else c.down

    ObchodnikCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        padding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetIcon(symbol = item.asset.symbol, colorHex = item.asset.colorHex)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.asset.symbol,
                    color = c.text,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${item.holding.qty} @ ${MarketFormatters.price(item.holding.avgPrice, currency)}",
                    color = c.text3,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = MarketFormatters.price(item.value, currency),
                    color = c.text,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = (if (item.plValue >= 0.0) "+" else "") + MarketFormatters.price(item.plValue, currency),
                    color = plColor,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Smazat pozici",
                    tint = c.down.copy(alpha = 0.72f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyPortfolio(onAddClick: () -> Unit) {
    val c = Obchodnik.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.FolderOpen,
            contentDescription = null,
            tint = c.text3,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Prázdné portfolio",
            color = c.text,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        Text(
            text = "Přidejte nákupy aktiv z watchlistu pro sledování hodnoty.",
            color = c.text3,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
        )
        Text(
            text = "Přidat první pozici",
            color = c.onAccent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier
                .background(c.accent, RoundedCornerShape(Obchodnik.radii.chip))
                .clickable { onAddClick() }
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PositionSheet(
    watchlist: List<Asset>,
    currency: String,
    editingItem: PortfolioItem?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double) -> Unit,
) {
    val c = Obchodnik.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditing = editingItem != null

    var selectedAsset by remember(editingItem, watchlist) {
        mutableStateOf(editingItem?.asset ?: watchlist.firstOrNull())
    }
    var qtyString by remember(editingItem) {
        mutableStateOf(editingItem?.holding?.qty?.toString().orEmpty())
    }
    var priceString by remember(editingItem) {
        mutableStateOf(editingItem?.holding?.avgPrice?.toString().orEmpty())
    }

    var expanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface)
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isEditing) "Upravit pozici" else "Přidat nákupní pozici",
                color = c.text,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(16.dp))

            // Dropdown selection
            if (watchlist.isEmpty()) {
                Text(
                    text = "Nejprve přidejte nějaké mince do watchlistu.",
                    color = c.down,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Text(
                    text = "Aktivum",
                    color = c.text2,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAsset?.let { "${it.symbol} - ${it.name}" } ?: "Vyberte...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.borderStrong,
                            focusedTextColor = c.text,
                            unfocusedTextColor = c.text
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        watchlist.forEach { asset ->
                            DropdownMenuItem(
                                text = { Text("${asset.symbol} - ${asset.name}") },
                                onClick = {
                                    selectedAsset = asset
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Quantity input
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Množství",
                        color = c.text2,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = qtyString,
                        onValueChange = { qtyString = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        placeholder = { Text("0.0", color = c.text3) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.borderStrong,
                            focusedTextColor = c.text,
                            unfocusedTextColor = c.text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Average Price input
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nákupní cena (${currency.uppercase()})",
                        color = c.text2,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = priceString,
                        onValueChange = { priceString = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        placeholder = { Text("0.00", color = c.text3) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.borderStrong,
                            focusedTextColor = c.text,
                            unfocusedTextColor = c.text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Confirm Button
            val qty = qtyString.toDoubleOrNull() ?: 0.0
            val price = priceString.toDoubleOrNull() ?: 0.0
            val isValid = selectedAsset != null && qty > 0.0 && price > 0.0

            Button(
                onClick = {
                    val asset = selectedAsset
                    if (asset != null && isValid) {
                        onConfirm(asset.id, qty, price)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.accent,
                    contentColor = c.onAccent,
                    disabledContainerColor = c.borderStrong,
                    disabledContentColor = c.text3
                ),
                shape = RoundedCornerShape(Obchodnik.radii.radius),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isEditing) "Uložit změny" else "Uložit pozici",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
