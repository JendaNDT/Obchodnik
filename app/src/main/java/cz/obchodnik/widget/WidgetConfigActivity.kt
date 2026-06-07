package cz.obchodnik.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.data.local.entity.AssetEntity
import cz.obchodnik.ui.components.AssetIcon
import cz.obchodnik.ui.components.ObchodnikCard
import cz.obchodnik.ui.theme.AccentChoice
import cz.obchodnik.ui.theme.JetBrainsMono
import cz.obchodnik.ui.theme.Obchodnik
import cz.obchodnik.ui.theme.ObchodnikTheme
import cz.obchodnik.ui.theme.ThemeChoice
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val app = application as ObchodnikApp
        val assetDao = app.container.database.assetDao()
        val settingsRepository = app.container.settingsRepository

        setContent {
            val settingsState by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val theme = settingsState?.let { s ->
                try { ThemeChoice.valueOf(s.theme.uppercase()) } catch (e: Exception) { ThemeChoice.TERMINAL }
            } ?: ThemeChoice.TERMINAL
            val accent = settingsState?.let { s ->
                try { AccentChoice.valueOf(s.accent.uppercase()) } catch (e: Exception) { AccentChoice.BLUE }
            } ?: AccentChoice.BLUE

            ObchodnikTheme(theme = theme, accent = accent) {
                WidgetConfigScreen(
                    assetDao = assetDao,
                    appWidgetId = appWidgetId,
                    onSaveCompleted = {
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    assetDao: cz.obchodnik.data.local.dao.AssetDao,
    appWidgetId: Int,
    onSaveCompleted: () -> Unit
) {
    val c = Obchodnik.colors
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var watchlistAssets by remember { mutableStateOf<List<AssetEntity>>(emptyList()) }
    val selectedAssetIds = remember { mutableStateListOf<String>() }
    var showFng by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf(WidgetMode.BALANCED) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Load watchlist assets
        watchlistAssets = assetDao.watchlistAssets()

        // Load existing widget preferences if any
        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val configAssetsStr = prefs[ObchodnikWidgetKeys.assets] ?: ""
        if (configAssetsStr.isNotBlank()) {
            selectedAssetIds.addAll(configAssetsStr.split(","))
        } else {
            // Pre-select first asset
            watchlistAssets.firstOrNull()?.let { selectedAssetIds.add(it.id) }
        }
        showFng = prefs[ObchodnikWidgetKeys.showFng] ?: true
        mode = WidgetMode.fromKey(prefs[ObchodnikWidgetKeys.mode])
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Konfigurace widgetu",
            color = c.text,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )

        Text(
            text = "Vyberte aktiva ze svého watchlistu, která se mají na widgetu zobrazovat. Zvolit lze nejvýše 5 aktiv.",
            color = c.text3,
            fontSize = 13.sp
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Načítání...", color = c.text2, fontFamily = JetBrainsMono)
            }
        } else if (watchlistAssets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Váš watchlist je prázdný. Přidejte nejprve aktiva v aplikaci.",
                    color = c.text3,
                    fontSize = 14.sp
                )
            }
        } else {
            ObchodnikCard(padding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                Column {
                    watchlistAssets.forEachIndexed { index, asset ->
                        val isSelected = selectedAssetIds.contains(asset.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        selectedAssetIds.remove(asset.id)
                                    } else {
                                        if (selectedAssetIds.size >= 5) {
                                            Toast.makeText(
                                                context,
                                                "Můžete vybrat maximálně 5 aktiv.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            selectedAssetIds.add(asset.id)
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssetIcon(symbol = asset.symbol, colorHex = asset.colorHex, size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = asset.symbol,
                                    color = c.text,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(text = asset.name, color = c.text3, fontSize = 11.sp)
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (selectedAssetIds.size >= 5) {
                                            Toast.makeText(
                                                context,
                                                "Můžete vybrat maximálně 5 aktiv.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            selectedAssetIds.add(asset.id)
                                        }
                                    } else {
                                        selectedAssetIds.remove(asset.id)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = c.accent,
                                    uncheckedColor = c.borderStrong,
                                    checkmarkColor = c.onAccent
                                )
                            )
                        }
                        if (index < watchlistAssets.lastIndex) {
                            HorizontalDivider(color = c.border, thickness = 1.dp)
                        }
                    }
                }
            }

            ObchodnikCard(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Režim widgetu",
                        color = c.text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetMode.entries.forEach { option ->
                            val active = option == mode
                            Text(
                                text = option.label,
                                color = if (active) c.onAccent else c.text2,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(
                                        if (active) c.accent else c.surface2,
                                        RoundedCornerShape(Obchodnik.radii.chip),
                                    )
                                    .clickable { mode = option }
                                    .padding(horizontal = 11.dp, vertical = 7.dp),
                            )
                        }
                    }
                    Text(
                        text = when (mode) {
                            WidgetMode.BALANCED -> "Cena, změna, mini grafy a volitelný Fear & Greed."
                            WidgetMode.PRICES -> "Více prostoru pro ceny bez mini grafů."
                            WidgetMode.CHARTS -> "Méně řádků, větší důraz na mini grafy."
                        },
                        color = c.text3,
                        fontSize = 11.sp,
                    )
                }
            }

            ObchodnikCard(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Zobrazit Fear & Greed index",
                            color = c.text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Aplikuje se pro střední a velké widgety",
                            color = c.text3,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = showFng,
                        onCheckedChange = { showFng = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = c.onAccent,
                            checkedTrackColor = c.accent,
                            uncheckedThumbColor = c.text3,
                            uncheckedTrackColor = c.surface2,
                            uncheckedBorderColor = c.borderStrong
                        )
                    )
                }
            }

            Button(
                onClick = {
                    if (selectedAssetIds.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Vyberte alespoň jedno aktivum.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    coroutineScope.launch {
                        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                        updateAppWidgetState(context, glanceId) { prefs ->
                            prefs[ObchodnikWidgetKeys.assets] = selectedAssetIds.joinToString(",")
                            prefs[ObchodnikWidgetKeys.showFng] = showFng
                            prefs[ObchodnikWidgetKeys.mode] = mode.key
                        }
                        ObchodnikWidget().update(context, glanceId)
                        onSaveCompleted()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(Obchodnik.radii.chip),
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.accent,
                    contentColor = c.onAccent
                )
            ) {
                Text(text = "Uložit widget", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
