package cz.obchodnik.ui.alerts

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.core.format.MarketFormatters
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.PriceAlert
import cz.obchodnik.ui.components.AssetIcon
import cz.obchodnik.ui.components.ObchodnikCard
import cz.obchodnik.ui.theme.JetBrainsMono
import cz.obchodnik.ui.theme.Obchodnik
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    state: AlertsUiState,
    onAddAlert: (String, Boolean, Double) -> Unit,
    onToggleAlert: (PriceAlert, Boolean) -> Unit,
    onDeleteAlert: (PriceAlert) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    var showSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
    ) {
        AlertsTopBar(onAddClick = { showSheet = true })

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LinearProgressIndicator(color = c.accent, trackColor = c.surface)
            }
        } else if (state.items.isEmpty()) {
            EmptyAlerts(onAddClick = { showSheet = true })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = state.items,
                    key = { it.alert.id }
                ) { item ->
                    AlertRow(
                        item = item,
                        currency = state.currency,
                        onToggle = { enabled -> onToggleAlert(item.alert, enabled) },
                        onDelete = { onDeleteAlert(item.alert) }
                    )
                }
            }
        }
    }

    if (showSheet) {
        AddAlertSheet(
            watchlist = state.allWatchlistAssets,
            currency = state.currency,
            onDismiss = { showSheet = false },
            onConfirm = { assetId, above, target ->
                onAddAlert(assetId, above, target)
                showSheet = false
            }
        )
    }
}

@Composable
private fun AlertsTopBar(onAddClick: () -> Unit) {
    val c = Obchodnik.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .padding(start = 16.dp, end = 4.dp, top = 48.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Alerty",
            color = c.text,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onAddClick) {
            Icon(Icons.Rounded.Add, contentDescription = "Nový alert", tint = c.text)
        }
    }
}

@Composable
private fun AlertRow(
    item: AlertItem,
    currency: String,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val c = Obchodnik.colors
    val conditionText = if (item.alert.above) "Cena nad" else "Cena pod"
    val formattedTarget = MarketFormatters.price(item.alert.target, currency)

    ObchodnikCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetIcon(symbol = item.asset.symbol, colorHex = item.asset.colorHex)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.asset.symbol,
                        color = c.text,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$conditionText $formattedTarget",
                        color = c.text2,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                val triggeredAt = item.alert.triggeredAt
                if (triggeredAt != null) {
                    val sdf = SimpleDateFormat("HH:mm dd.MM.", Locale.getDefault())
                    val dateStr = sdf.format(Date(triggeredAt))
                    Text(
                        text = "Spuštěno: $dateStr",
                        color = c.down,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (item.alert.enabled) {
                    Text(
                        text = "Aktivní",
                        color = c.up,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Vypnuto",
                        color = c.text3,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = item.alert.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.onAccent,
                    checkedTrackColor = c.accent,
                    uncheckedThumbColor = c.text3,
                    uncheckedTrackColor = c.surface
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Smazat alert",
                    tint = c.text3,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyAlerts(onAddClick: () -> Unit) {
    val c = Obchodnik.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.NotificationsNone,
            contentDescription = null,
            tint = c.text3,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Žádné cenové alerty",
            color = c.text,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        Text(
            text = "Nastavte si upozornění na překročení cílové ceny aktiva. Dohlédneme na to na pozadí.",
            color = c.text3,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
        )
        Text(
            text = "Vytvořit první alert",
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
private fun AddAlertSheet(
    watchlist: List<Asset>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, Double) -> Unit,
) {
    val c = Obchodnik.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedAsset by remember { mutableStateOf<Asset?>(watchlist.firstOrNull()) }
    var targetPriceString by remember { mutableStateOf("") }
    var isAbove by remember { mutableStateOf(true) }

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
                text = "Nový cenový alert",
                color = c.text,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(16.dp))

            if (watchlist.isEmpty()) {
                Text(
                    text = "Nejprve přidejte mince do watchlistu.",
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

            // Direction Selection (Nad / Pod)
            Text(
                text = "Upozornit při překročení",
                color = c.text2,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(Obchodnik.radii.radiusSm))
                    .background(c.borderStrong)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(Obchodnik.radii.radiusSm))
                        .background(if (isAbove) c.accent else Color.Transparent)
                        .clickable { isAbove = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cena roste NAD",
                        color = if (isAbove) c.onAccent else c.text2,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(Obchodnik.radii.radiusSm))
                        .background(if (!isAbove) c.accent else Color.Transparent)
                        .clickable { isAbove = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cena klesá POD",
                        color = if (!isAbove) c.onAccent else c.text2,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Target Price Input
            Text(
                text = "Cílová cena (${currency.uppercase()})",
                color = c.text2,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = targetPriceString,
                onValueChange = { targetPriceString = it },
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

            Spacer(Modifier.height(24.dp))

            // Create Button
            val targetPrice = targetPriceString.toDoubleOrNull() ?: 0.0
            val isValid = selectedAsset != null && targetPrice > 0.0

            Button(
                onClick = {
                    val asset = selectedAsset
                    if (asset != null && isValid) {
                        onConfirm(asset.id, isAbove, targetPrice)
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
                Text(text = "Uložit alert", fontWeight = FontWeight.Bold)
            }
        }
    }
}
