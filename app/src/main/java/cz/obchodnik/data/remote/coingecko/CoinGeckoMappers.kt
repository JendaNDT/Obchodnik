package cz.obchodnik.data.remote.coingecko

import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoMarketChartDto
import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoMarketCoinDto
import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoSearchCoinDto
import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote
import java.time.Instant

private val tokenizedMetalIds = setOf("pax-gold", "tether-gold")

private val knownAssetColors = mapOf(
    "BTC" to "#f7931a",
    "ETH" to "#627eea",
    "SOL" to "#14f195",
    "BNB" to "#f3ba2f",
    "XRP" to "#23292f",
    "ADA" to "#0033ad",
    "DOGE" to "#c2a633",
    "AVAX" to "#e84142",
    "PAXG" to "#d4af37",
    "XAUT" to "#d4af37",
)

fun CoinGeckoSearchCoinDto.toAsset(): Asset {
    val normalizedSymbol = symbol.uppercase()
    return Asset(
        id = coingeckoAssetId(id),
        symbol = normalizedSymbol,
        name = name,
        type = assetTypeForCoinGeckoId(id),
        source = DataProvider.COINGECKO,
        sourceId = id,
        colorHex = knownAssetColors[normalizedSymbol],
        logoUrl = large ?: thumb,
    )
}

fun CoinGeckoMarketCoinDto.toAsset(): Asset {
    val normalizedSymbol = symbol.uppercase()
    return Asset(
        id = coingeckoAssetId(id),
        symbol = normalizedSymbol,
        name = name,
        type = assetTypeForCoinGeckoId(id),
        source = DataProvider.COINGECKO,
        sourceId = id,
        colorHex = knownAssetColors[normalizedSymbol],
        logoUrl = image,
    )
}

fun CoinGeckoMarketCoinDto.toQuote(currency: String, fallbackUpdatedAt: Long = System.currentTimeMillis()): Quote =
    Quote(
        assetId = coingeckoAssetId(id),
        price = currentPrice ?: 0.0,
        change24hPct = priceChangePercentage24hInCurrency ?: priceChangePercentage24h,
        change7dPct = priceChangePercentage7dInCurrency,
        change30dPct = priceChangePercentage30dInCurrency,
        high24h = high24h,
        low24h = low24h,
        marketCap = marketCap,
        volume24h = totalVolume,
        sparkline7d = sparklineIn7d?.price.orEmpty(),
        currency = currency.lowercase(),
        updatedAt = parseIsoTimestamp(lastUpdated) ?: fallbackUpdatedAt,
    )

fun CoinGeckoMarketChartDto.toPricePoints(): List<PricePoint> =
    prices.mapNotNull { row ->
        if (row.size >= 2) {
            PricePoint(timestamp = row[0].toLong(), price = row[1])
        } else {
            null
        }
    }

fun List<List<Double>>.toCandles(): List<Candle> =
    mapNotNull { row ->
        if (row.size >= 5) {
            Candle(
                timestamp = row[0].toLong(),
                open = row[1],
                high = row[2],
                low = row[3],
                close = row[4],
            )
        } else {
            null
        }
    }

fun coingeckoAssetId(id: String): String = "cg:$id"

private fun assetTypeForCoinGeckoId(id: String): AssetType =
    if (id in tokenizedMetalIds) AssetType.METAL else AssetType.CRYPTO

private fun parseIsoTimestamp(value: String?): Long? =
    value?.let {
        runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
    }
