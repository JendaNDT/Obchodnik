package cz.obchodnik.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.data.backup.BackupImportPreview
import cz.obchodnik.ui.components.ObchodnikCard
import cz.obchodnik.ui.theme.AccentChoice
import cz.obchodnik.ui.theme.JetBrainsMono
import cz.obchodnik.ui.theme.Obchodnik
import cz.obchodnik.ui.theme.ThemeChoice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeChanged: (String) -> Unit,
    onAccentChanged: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onRefreshIntervalChanged: (Int) -> Unit,
    onDefaultChartChanged: (String) -> Unit,
    onDensityChanged: (String) -> Unit,
    onShowFngOnWidgetChanged: (Boolean) -> Unit,
    onCoinGeckoKeyChanged: (String) -> Unit,
    onAlphaVantageKeyChanged: (String) -> Unit,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    onResetOnboarding: () -> Unit,
    onExportData: (Uri, Boolean) -> Unit,
    onPreviewImportData: (Uri) -> Unit,
    onConfirmImportData: () -> Unit,
    onDismissImportPreview: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    val scrollState = rememberScrollState()
    var includeApiKeysInBackup by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { onExportData(it, includeApiKeysInBackup) } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onPreviewImportData) }

    state.importPreview?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            onConfirm = onConfirmImportData,
            onDismiss = onDismissImportPreview,
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = c.surface,
            titleContentColor = c.text,
            textContentColor = c.text2,
            title = {
                Text(text = "Resetovat onboarding", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = "Opravdu chcete resetovat průvodce? Při příštím spuštění aplikace se zobrazí úvodní obrazovky.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetOnboarding()
                        showResetDialog = false
                    },
                    shape = RoundedCornerShape(Obchodnik.radii.radius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.accent,
                        contentColor = c.onAccent,
                    ),
                ) {
                    Text(text = "Resetovat", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = "Zrušit", color = c.text2, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        // TopBar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 48.dp, bottom = 8.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět", tint = c.text)
            }
            Text(
                text = "Nastavení",
                color = c.text,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Načítání...", color = c.text2, fontFamily = JetBrainsMono)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // SECTION: ZOBRAZENÍ
                SettingsSection(title = "Zobrazení") {
                    ObchodnikCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Theme selector
                            SettingsRow(title = "Téma", subtitle = "Vizuální motiv aplikace") {
                                val themes = listOf(
                                    ThemeChoice.TERMINAL to "terminal",
                                    ThemeChoice.AURORA to "aurora",
                                    ThemeChoice.MONO to "mono"
                                )
                                SegmentedControl(
                                    options = themes.map { it.first.labelCs },
                                    selectedOption = themes.firstOrNull { it.second == state.settings.theme }?.first?.labelCs ?: "Terminal",
                                    onOptionSelected = { label ->
                                        themes.firstOrNull { it.first.labelCs == label }?.second?.let(onThemeChanged)
                                    }
                                )
                            }

                            // Accent selector
                            SettingsRow(title = "Akcent", subtitle = "Hlavní zvýrazňující barva") {
                                AccentSelector(
                                    selectedAccent = state.settings.accent,
                                    onAccentSelected = onAccentChanged
                                )
                            }

                            // Density selector
                            SettingsRow(title = "Hustota seznamů", subtitle = "Hustota řádků na hlavní obrazovce") {
                                val options = listOf("normal" to "Normální", "compact" to "Kompaktní")
                                SegmentedControl(
                                    options = options.map { it.second },
                                    selectedOption = options.firstOrNull { it.first == state.settings.density }?.second ?: "Normální",
                                    onOptionSelected = { label ->
                                        options.firstOrNull { it.second == label }?.first?.let(onDensityChanged)
                                    }
                                )
                            }

                            // Default chart selector
                            SettingsRow(title = "Výchozí graf", subtitle = "Styl grafu v detailu aktiva") {
                                val options = listOf("line" to "Křivka", "candle" to "Svíčky")
                                SegmentedControl(
                                    options = options.map { it.second },
                                    selectedOption = options.firstOrNull { it.first == state.settings.defaultChart }?.second ?: "Křivka",
                                    onOptionSelected = { label ->
                                        options.firstOrNull { it.second == label }?.first?.let(onDefaultChartChanged)
                                    }
                                )
                            }
                        }
                    }
                }

                // SECTION: MĚNA & JEDNOTKY
                SettingsSection(title = "Měna & jednotky") {
                    ObchodnikCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        SettingsRow(title = "Výchozí měna", subtitle = "Ceny aktiv budou zobrazeny v této měně") {
                            val options = listOf("usd" to "USD ($)", "czk" to "CZK (Kč)")
                            SegmentedControl(
                                options = options.map { it.second },
                                selectedOption = options.firstOrNull { it.first == state.settings.currency }?.second ?: "USD ($)",
                                onOptionSelected = { label ->
                                    options.firstOrNull { it.second == label }?.first?.let(onCurrencyChanged)
                                }
                            )
                        }
                    }
                }

                // SECTION: AKTUALIZACE
                SettingsSection(title = "Aktualizace") {
                    ObchodnikCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "Interval aktualizace na pozadí",
                                color = c.text,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Poznámka: Systém Android omezuje periodické úlohy na minimálně 15 minut.",
                                color = c.text3,
                                fontSize = 11.sp
                            )
                            val intervals = listOf(
                                0 to "Jen při otevření",
                                15 to "15 min",
                                30 to "30 min",
                                60 to "1 hod",
                                180 to "3 hod",
                                360 to "6 hod"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                intervals.forEach { (minutes, label) ->
                                    val isSelected = state.settings.refreshIntervalMinutes == minutes
                                    Text(
                                        text = label,
                                        color = if (isSelected) c.onAccent else c.text2,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .background(
                                                color = if (isSelected) c.accent else c.surface2,
                                                shape = RoundedCornerShape(Obchodnik.radii.chip)
                                            )
                                            .border(
                                                BorderStroke(1.dp, if (isSelected) c.accent else c.border),
                                                RoundedCornerShape(Obchodnik.radii.chip)
                                            )
                                            .clickable { onRefreshIntervalChanged(minutes) }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            SettingsRow(
                                title = "Zobrazovat F&G na widgetu",
                                subtitle = "Ukáže stav Fear & Greed indexu"
                            ) {
                                ObchodnikSwitch(
                                    checked = state.settings.showFngOnWidget,
                                    onCheckedChange = onShowFngOnWidgetChanged
                                )
                            }
                        }
                    }
                }

                // SECTION: ZDROJ DAT & KLÍČE
                SettingsSection(title = "Zdroj dat & klíče") {
                    ObchodnikCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            Column {
                                Text(
                                    text = "CoinGecko Demo API klíč",
                                    color = c.text,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Kryptoměny. Volitelné, bez klíče platí nižší limity požadavků.",
                                    color = c.text3,
                                    fontSize = 11.sp,
                                )
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Získat Demo klíč zdarma ↗",
                                        color = c.accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            runCatching { uriHandler.openUri("https://developer.coingecko.com/") }
                                        }
                                    )
                                }
                                SettingsTextField(
                                    value = state.settings.coingeckoKey,
                                    onValueChange = onCoinGeckoKeyChanged,
                                    placeholder = "Zadejte CoinGecko Demo klíč"
                                )
                            }

                            Column {
                                Text(
                                    text = "Alpha Vantage API klíč",
                                    color = c.text,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Komodity a indexy. Nutné pro jejich správné načítání.",
                                    color = c.text3,
                                    fontSize = 11.sp,
                                )
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Získat bezplatný klíč ↗",
                                        color = c.accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            runCatching { uriHandler.openUri("https://www.alphavantage.co/support/#api-key") }
                                        }
                                    )
                                }
                                SettingsTextField(
                                    value = state.settings.alphaVantageKey,
                                    onValueChange = onAlphaVantageKeyChanged,
                                    placeholder = "Zadejte Alpha Vantage klíč"
                                )
                            }
                        }
                    }
                }

                // SECTION: OZNÁMENÍ
                SettingsSection(title = "Oznámení") {
                    ObchodnikCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        SettingsRow(
                            title = "Upozornění na ceny",
                            subtitle = "Povolit notifikace při splnění cenových alertů"
                        ) {
                            ObchodnikSwitch(
                                checked = state.settings.notificationsEnabled,
                                onCheckedChange = onNotificationsEnabledChanged
                            )
                        }
                    }
                }

                // SECTION: ZÁLOHA DAT
                SettingsSection(title = "Záloha dat") {
                    ObchodnikCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "Exportuje watchlist, portfolio, alerty a nastavení do souboru JSON. Import přidá uložené položky k aktuálním.",
                                color = c.text3,
                                fontSize = 11.sp,
                            )
                            SettingsRow(
                                title = "Přidat API klíče do exportu",
                                subtitle = "Vypnuto znamená bezpečnější zálohu bez citlivých údajů"
                            ) {
                                ObchodnikSwitch(
                                    checked = includeApiKeysInBackup,
                                    onCheckedChange = { includeApiKeysInBackup = it },
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Button(
                                    onClick = { exportLauncher.launch("obchodnik-zaloha.json") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(Obchodnik.radii.radius),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = c.accent,
                                        contentColor = c.onAccent,
                                    ),
                                ) {
                                    Text(text = "Exportovat", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = {
                                        importLauncher.launch(
                                            arrayOf("application/json", "application/octet-stream", "text/plain"),
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(Obchodnik.radii.radius),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = c.surface2,
                                        contentColor = c.text,
                                    ),
                                    border = BorderStroke(1.dp, c.borderStrong),
                                ) {
                                    Text(text = "Importovat", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // SECTION: APLIKACE
                SettingsSection(title = "Aplikace") {
                    ObchodnikCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SettingsRow(
                                title = "Resetovat onboarding",
                                subtitle = "Znovu spustí úvodního průvodce při příštím zapnutí"
                            ) {
                                Button(
                                    onClick = { showResetDialog = true },
                                    shape = RoundedCornerShape(Obchodnik.radii.radius),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = c.surface2,
                                        contentColor = c.text
                                    ),
                                    border = BorderStroke(1.dp, c.borderStrong),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(text = "Reset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Verze",
                                        color = c.text,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Nativní Android Obchodník klient",
                                        color = c.text3,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "1.0.0 (Debug)",
                                    color = c.text2,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun ImportPreviewDialog(
    preview: BackupImportPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Obchodnik.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        titleContentColor = c.text,
        textContentColor = c.text2,
        title = {
            Text(
                text = "Náhled importu",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Import přidá položky ze souboru k aktuálním datům.",
                    color = c.text2,
                    fontSize = 13.sp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ImportPreviewLine("Verze schématu", preview.schemaVersion.toString())
                    if (preview.appVersion.isNotBlank()) {
                        ImportPreviewLine("Verze aplikace", preview.appVersion)
                    }
                    ImportPreviewLine("Exportováno", formatBackupTimestamp(preview.exportedAt))
                    ImportPreviewLine(
                        "Aktiva",
                        if (preview.importableAssets == preview.assets) {
                            preview.assets.toString()
                        } else {
                            "${preview.importableAssets} z ${preview.assets} použitelných"
                        },
                    )
                    ImportPreviewLine("Pozice", preview.holdings.toString())
                    ImportPreviewLine("Alerty", preview.alerts.toString())
                    ImportPreviewLine(
                        "API klíče",
                        if (preview.includesApiKeys) "součástí zálohy" else "neobsahuje",
                    )
                }
                if (preview.importableAssets < preview.assets) {
                    Text(
                        text = "Některá aktiva mají neznámý typ nebo zdroj a přeskočí se.",
                        color = c.down,
                        fontSize = 12.sp,
                    )
                }
                if (preview.includesApiKeys) {
                    Text(
                        text = "Soubor obsahuje API klíče. Import je uloží do tohoto zařízení.",
                        color = c.text3,
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(Obchodnik.radii.radius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.accent,
                    contentColor = c.onAccent,
                ),
            ) {
                Text(text = "Importovat", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Zrušit", color = c.text2, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun ImportPreviewLine(
    label: String,
    value: String,
) {
    val c = Obchodnik.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = c.text3, fontSize = 12.sp)
        Text(
            text = value,
            color = c.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = JetBrainsMono,
        )
    }
}

private fun formatBackupTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0L) return "neuvedeno"
    return SimpleDateFormat("d. M. yyyy HH:mm", Locale("cs", "CZ")).format(Date(epochMillis))
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Obchodnik.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            color = c.text3,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Column(content = content)
    }
}

@Composable
private fun ColumnScope.SettingsRow(
    title: String,
    subtitle: String? = null,
    action: @Composable () -> Unit,
) {
    val c = Obchodnik.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = c.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = c.text3,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        action()
    }
}

@Composable
private fun SegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
) {
    val c = Obchodnik.colors
    Row(
        modifier = Modifier
            .background(c.surface2, RoundedCornerShape(Obchodnik.radii.chip))
            .border(BorderStroke(1.dp, c.border), RoundedCornerShape(Obchodnik.radii.chip))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val isActive = option == selectedOption
            Text(
                text = option,
                color = if (isActive) c.onAccent else c.text2,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier
                    .background(
                        color = if (isActive) c.accent else Color.Transparent,
                        shape = RoundedCornerShape(Obchodnik.radii.chip)
                    )
                    .clickable { onOptionSelected(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun AccentSelector(
    selectedAccent: String,
    onAccentSelected: (String) -> Unit,
) {
    val c = Obchodnik.colors
    val accents = listOf(
        AccentChoice.BLUE to "blue",
        AccentChoice.GREEN to "green",
        AccentChoice.ORANGE to "orange",
        AccentChoice.PURPLE to "purple"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        accents.forEach { (accentChoice, name) ->
            val isSelected = name == selectedAccent
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentChoice.color)
                    .border(
                        BorderStroke(
                            if (isSelected) 2.dp else 0.dp,
                            if (isSelected) c.text else Color.Transparent
                        ),
                        CircleShape
                    )
                    .clickable { onAccentSelected(name) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ObchodnikSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = Obchodnik.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onAccent,
            checkedTrackColor = c.accent,
            uncheckedThumbColor = c.text3,
            uncheckedTrackColor = c.surface2,
            uncheckedBorderColor = c.borderStrong,
        )
    )
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val c = Obchodnik.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = c.text3, fontSize = 13.sp) },
        shape = RoundedCornerShape(Obchodnik.radii.chip),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            color = c.text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = c.text,
            unfocusedTextColor = c.text,
            focusedContainerColor = c.surface2,
            unfocusedContainerColor = c.surface2,
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.borderStrong,
            cursorColor = c.accent,
        )
    )
}
