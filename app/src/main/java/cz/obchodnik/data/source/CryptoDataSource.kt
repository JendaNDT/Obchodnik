package cz.obchodnik.data.source

import cz.obchodnik.core.Result
import cz.obchodnik.data.remote.coingecko.CoinGeckoApi
import cz.obchodnik.data.remote.coingecko.toAsset
import cz.obchodnik.data.remote.coingecko.toCandles
import cz.obchodnik.data.remote.coingecko.toPricePoints
import cz.obchodnik.data.remote.coingecko.toQuote
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote

class CryptoDataSource(
    private val api: CoinGeckoApi,
) : MarketDataSource {
    override suspend fun quotes(assets: List<Asset>, currency: String): Result<List<Quote>> =
        runCatching {
            val coinGeckoAssets = assets.filter { it.source == DataProvider.COINGECKO }
            if (coinGeckoAssets.isEmpty()) {
                emptyList()
            } else {
                val ids = coinGeckoAssets.joinToString(",") { it.sourceId }
                api.markets(vsCurrency = currency.lowercase(), ids = ids)
                    .map { it.toQuote(currency = currency) }
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error("CoinGecko quotes failed", it) },
        )

    override suspend fun history(asset: Asset, range: ChartRange, currency: String): Result<List<PricePoint>> =
        if (asset.source != DataProvider.COINGECKO) {
            Result.Error("Data pro tento zdroj zatím nejsou dostupná")
        } else {
        runCatching {
            api.marketChart(
                id = asset.sourceId,
                vsCurrency = currency.lowercase(),
                days = range.coingeckoMarketChartDays,
            ).toPricePoints()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error("CoinGecko history failed", it) },
        )
        }

    override suspend fun candles(asset: Asset, range: ChartRange, currency: String): Result<List<Candle>> =
        if (asset.source != DataProvider.COINGECKO) {
            Result.Error("Data pro tento zdroj zatím nejsou dostupná")
        } else {
        runCatching {
            api.ohlc(
                id = asset.sourceId,
                vsCurrency = currency.lowercase(),
                days = range.coingeckoOhlcDays,
            ).toCandles()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error("CoinGecko candles failed", it) },
        )
        }

    override suspend fun search(query: String): Result<List<Asset>> =
        runCatching {
            if (query.isBlank()) {
                emptyList()
            } else {
                api.search(query.trim()).coins.map { it.toAsset() }
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error("CoinGecko search failed", it) },
        )
}
