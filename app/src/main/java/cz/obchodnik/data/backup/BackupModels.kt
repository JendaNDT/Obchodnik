package cz.obchodnik.data.backup

import cz.obchodnik.data.prefs.AppSettings
import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.DataProvider
import kotlinx.serialization.Serializable

/** Bump when the backup JSON structure changes incompatibly. */
const val BACKUP_SCHEMA_VERSION: Int = 1

@Serializable
data class BackupData(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAt: Long = 0L,
    val appVersion: String = "",
    val watchlist: List<BackupAsset> = emptyList(),
    val holdings: List<BackupHolding> = emptyList(),
    val alerts: List<BackupAlert> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
)

@Serializable
data class BackupAsset(
    val id: String,
    val symbol: String,
    val name: String,
    val type: String,
    val source: String,
    val sourceId: String,
    val colorHex: String? = null,
    val logoUrl: String? = null,
)

@Serializable
data class BackupHolding(
    val assetId: String,
    val qty: Double,
    val avgPrice: Double,
)

@Serializable
data class BackupAlert(
    val assetId: String,
    val above: Boolean,
    val target: Double,
    val enabled: Boolean,
    val triggeredAt: Long? = null,
    val triggeredPrice: Double? = null,
    val triggeredCurrency: String? = null,
)

@Serializable
data class BackupSettings(
    val theme: String = "terminal",
    val accent: String = "blue",
    val currency: String = "usd",
    val refreshIntervalMinutes: Int = 30,
    val defaultChart: String = "line",
    val density: String = "normal",
    val showFngOnWidget: Boolean = true,
    val coingeckoKey: String? = null,
    val alphaVantageKey: String? = null,
    val notificationsEnabled: Boolean = false,
)

fun Asset.toBackup(): BackupAsset =
    BackupAsset(
        id = id,
        symbol = symbol,
        name = name,
        type = type.name,
        source = source.name,
        sourceId = sourceId,
        colorHex = colorHex,
        logoUrl = logoUrl,
    )

/** Returns null when the stored enum names are unknown (corrupt/foreign backup). */
fun BackupAsset.toAssetOrNull(): Asset? {
    val assetType = runCatching { AssetType.valueOf(type) }.getOrNull() ?: return null
    val provider = runCatching { DataProvider.valueOf(source) }.getOrNull() ?: return null
    return Asset(
        id = id,
        symbol = symbol,
        name = name,
        type = assetType,
        source = provider,
        sourceId = sourceId,
        colorHex = colorHex,
        logoUrl = logoUrl,
    )
}

fun AppSettings.toBackup(includeApiKeys: Boolean = false): BackupSettings =
    BackupSettings(
        theme = theme,
        accent = accent,
        currency = currency,
        refreshIntervalMinutes = refreshIntervalMinutes,
        defaultChart = defaultChart,
        density = density,
        showFngOnWidget = showFngOnWidget,
        coingeckoKey = coingeckoKey.takeIf { includeApiKeys },
        alphaVantageKey = alphaVantageKey.takeIf { includeApiKeys },
        notificationsEnabled = notificationsEnabled,
    )
