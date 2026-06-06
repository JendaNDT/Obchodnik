package cz.obchodnik.data.backup

import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.repository.AlertRepository
import cz.obchodnik.data.repository.PortfolioRepository
import cz.obchodnik.data.repository.WatchlistRepository
import cz.obchodnik.domain.model.Holding
import cz.obchodnik.domain.model.PriceAlert
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

data class ImportSummary(
    val assets: Int,
    val holdings: Int,
    val alerts: Int,
)

/**
 * Exports user data (watchlist, holdings, alerts, user settings) to a JSON
 * string and restores it. Cached quotes/history and runtime counters (Alpha
 * Vantage budget, FX rate, onboarding flag) are intentionally left out; they
 * are re-fetched or device-local. Import appends rows, so importing the same
 * file twice can create duplicate holdings/alerts.
 */
class BackupRepository(
    private val watchlistRepository: WatchlistRepository,
    private val portfolioRepository: PortfolioRepository,
    private val alertRepository: AlertRepository,
    private val settingsRepository: SettingsRepository,
    private val json: Json,
    private val appVersion: String,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun buildBackup(): BackupData {
        val settings = settingsRepository.settings.first()
        return BackupData(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = now(),
            appVersion = appVersion,
            watchlist = watchlistRepository.watchlist().map { it.toBackup() },
            holdings = portfolioRepository.observeHoldings().first().map {
                BackupHolding(assetId = it.assetId, qty = it.qty, avgPrice = it.avgPrice)
            },
            alerts = alertRepository.observeAlerts().first().map {
                BackupAlert(assetId = it.assetId, above = it.above, target = it.target, enabled = it.enabled)
            },
            settings = settings.toBackup(),
        )
    }

    fun encode(data: BackupData): String =
        json.encodeToString(BackupData.serializer(), data)

    fun decode(text: String): BackupData =
        json.decodeFromString(BackupData.serializer(), text)

    suspend fun exportToJson(): String = encode(buildBackup())

    suspend fun importFromJson(text: String): ImportSummary = import(decode(text))

    suspend fun import(data: BackupData): ImportSummary {
        var importedAssets = 0
        data.watchlist.forEach { backupAsset ->
            val asset = backupAsset.toAssetOrNull() ?: return@forEach
            watchlistRepository.add(asset)
            importedAssets++
        }
        data.holdings.forEach { holding ->
            portfolioRepository.save(
                Holding(id = 0L, assetId = holding.assetId, qty = holding.qty, avgPrice = holding.avgPrice),
            )
        }
        data.alerts.forEach { alert ->
            alertRepository.save(
                PriceAlert(
                    id = 0L,
                    assetId = alert.assetId,
                    above = alert.above,
                    target = alert.target,
                    enabled = alert.enabled,
                    triggeredAt = null,
                ),
            )
        }
        applySettings(data.settings)
        return ImportSummary(
            assets = importedAssets,
            holdings = data.holdings.size,
            alerts = data.alerts.size,
        )
    }

    private suspend fun applySettings(settings: BackupSettings) {
        settingsRepository.setTheme(settings.theme)
        settingsRepository.setAccent(settings.accent)
        settingsRepository.setCurrency(settings.currency)
        settingsRepository.setRefreshIntervalMinutes(settings.refreshIntervalMinutes)
        settingsRepository.setDefaultChart(settings.defaultChart)
        settingsRepository.setDensity(settings.density)
        settingsRepository.setShowFngOnWidget(settings.showFngOnWidget)
        settingsRepository.setCoinGeckoKey(settings.coingeckoKey)
        settingsRepository.setAlphaVantageKey(settings.alphaVantageKey)
        settingsRepository.setNotificationsEnabled(settings.notificationsEnabled)
    }
}
