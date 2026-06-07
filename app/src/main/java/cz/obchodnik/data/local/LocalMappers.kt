package cz.obchodnik.data.local

import cz.obchodnik.data.local.entity.AlertEntity
import cz.obchodnik.data.local.entity.AssetEntity
import cz.obchodnik.data.local.entity.HoldingEntity
import cz.obchodnik.data.local.entity.HistoryEntity
import cz.obchodnik.data.local.entity.PortfolioSnapshotEntity
import cz.obchodnik.data.local.entity.QuoteEntity
import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.Holding
import cz.obchodnik.domain.model.PriceAlert
import cz.obchodnik.domain.model.PortfolioSnapshot
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Asset.toEntity(inWatchlist: Boolean = false, sortOrder: Int = Int.MAX_VALUE): AssetEntity =
    AssetEntity(
        id = id,
        symbol = symbol,
        name = name,
        type = type.name,
        provider = source.name,
        sourceId = sourceId,
        colorHex = colorHex,
        logoUrl = logoUrl,
        inWatchlist = inWatchlist,
        sortOrder = sortOrder,
    )

fun AssetEntity.toDomain(): Asset =
    Asset(
        id = id,
        symbol = symbol,
        name = name,
        type = AssetType.valueOf(type),
        source = DataProvider.valueOf(provider),
        sourceId = sourceId,
        colorHex = colorHex,
        logoUrl = logoUrl,
    )

fun Quote.toEntity(json: Json): QuoteEntity =
    QuoteEntity(
        assetId = assetId,
        currency = currency.lowercase(),
        price = price,
        change24h = change24hPct,
        change7d = change7dPct,
        change30d = change30dPct,
        high24h = high24h,
        low24h = low24h,
        marketCap = marketCap,
        volume24h = volume24h,
        sparklineJson = json.encodeToString(sparkline7d),
        updatedAt = updatedAt,
    )

fun QuoteEntity.toDomain(json: Json): Quote =
    Quote(
        assetId = assetId,
        price = price,
        change24hPct = change24h,
        change7dPct = change7d,
        change30dPct = change30d,
        high24h = high24h,
        low24h = low24h,
        marketCap = marketCap,
        volume24h = volume24h,
        sparkline7d = runCatching { json.decodeFromString<List<Double>>(sparklineJson) }.getOrDefault(emptyList()),
        currency = currency,
        updatedAt = updatedAt,
    )

fun HoldingEntity.toDomain(): Holding =
    Holding(id = id, assetId = assetId, qty = qty, avgPrice = avgPrice)

fun Holding.toEntity(): HoldingEntity =
    HoldingEntity(id = id, assetId = assetId, qty = qty, avgPrice = avgPrice)

fun AlertEntity.toDomain(): PriceAlert =
    PriceAlert(
        id = id,
        assetId = assetId,
        above = above,
        target = target,
        enabled = enabled,
        triggeredAt = triggeredAt,
        triggeredPrice = triggeredPrice,
        triggeredCurrency = triggeredCurrency,
        repeating = repeating,
        armed = armed,
    )

fun PriceAlert.toEntity(): AlertEntity =
    AlertEntity(
        id = id,
        assetId = assetId,
        above = above,
        target = target,
        enabled = enabled,
        triggeredAt = triggeredAt,
        triggeredPrice = triggeredPrice,
        triggeredCurrency = triggeredCurrency,
        repeating = repeating,
        armed = armed,
    )

fun pricePointsToEntity(
    assetId: String,
    range: ChartRange,
    currency: String,
    points: List<PricePoint>,
    updatedAt: Long,
    json: Json,
): HistoryEntity =
    HistoryEntity(
        assetId = assetId,
        range = range.name,
        currency = currency.lowercase(),
        kind = "line",
        pointsJson = json.encodeToString(points),
        updatedAt = updatedAt,
    )

fun candlesToEntity(
    assetId: String,
    range: ChartRange,
    currency: String,
    candles: List<Candle>,
    updatedAt: Long,
    json: Json,
): HistoryEntity =
    HistoryEntity(
        assetId = assetId,
        range = range.name,
        currency = currency.lowercase(),
        kind = "candle",
        pointsJson = json.encodeToString(candles),
        updatedAt = updatedAt,
    )

fun HistoryEntity.toPricePoints(json: Json): List<PricePoint> =
    runCatching { json.decodeFromString<List<PricePoint>>(pointsJson) }.getOrDefault(emptyList())

fun HistoryEntity.toCandles(json: Json): List<Candle> =
    runCatching { json.decodeFromString<List<Candle>>(pointsJson) }.getOrDefault(emptyList())

fun PortfolioSnapshotEntity.toDomain(): PortfolioSnapshot =
    PortfolioSnapshot(
        id = id,
        dayStartMillis = dayStartMillis,
        totalValue = totalValue,
        totalInvested = totalInvested,
        currency = currency,
    )

fun PortfolioSnapshot.toEntity(): PortfolioSnapshotEntity =
    PortfolioSnapshotEntity(
        id = id,
        dayStartMillis = dayStartMillis,
        totalValue = totalValue,
        totalInvested = totalInvested,
        currency = currency,
    )
