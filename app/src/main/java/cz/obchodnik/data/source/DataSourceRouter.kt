package cz.obchodnik.data.source

import cz.obchodnik.core.Result
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DataSourceRouter(
    private val cryptoDataSource: MarketDataSource,
    private val commodityDataSource: MarketDataSource,
) : MarketDataSource {

    override suspend fun quotes(assets: List<Asset>, currency: String): Result<List<Quote>> = coroutineScope {
        val cryptoAssets = assets.filter { it.source == DataProvider.COINGECKO }
        val avAssets = assets.filter { it.source == DataProvider.ALPHAVANTAGE }

        val cryptoDeferred = async { cryptoDataSource.quotes(cryptoAssets, currency) }
        val avDeferred = async { commodityDataSource.quotes(avAssets, currency) }

        val cryptoResult = cryptoDeferred.await()
        val avResult = avDeferred.await()

        val results = mutableListOf<Quote>()
        var errorMessage: String? = null
        var errorCause: Throwable? = null

        when (cryptoResult) {
            is Result.Success -> results.addAll(cryptoResult.data)
            is Result.Error -> {
                errorMessage = cryptoResult.message
                errorCause = cryptoResult.cause
            }
            Result.Loading -> {}
        }

        when (avResult) {
            is Result.Success -> results.addAll(avResult.data)
            is Result.Error -> {
                errorMessage = avResult.message
                errorCause = avResult.cause
            }
            Result.Loading -> {}
        }

        if (results.isEmpty() && errorMessage != null) {
            Result.Error(errorMessage, errorCause)
        } else {
            Result.Success(results, notice = errorMessage)
        }
    }

    override suspend fun history(asset: Asset, range: ChartRange, currency: String): Result<List<PricePoint>> {
        return when (asset.source) {
            DataProvider.COINGECKO -> cryptoDataSource.history(asset, range, currency)
            DataProvider.ALPHAVANTAGE -> commodityDataSource.history(asset, range, currency)
        }
    }

    override suspend fun candles(asset: Asset, range: ChartRange, currency: String): Result<List<Candle>> {
        return when (asset.source) {
            DataProvider.COINGECKO -> cryptoDataSource.candles(asset, range, currency)
            DataProvider.ALPHAVANTAGE -> commodityDataSource.candles(asset, range, currency)
        }
    }

    override suspend fun search(query: String): Result<List<Asset>> {
        return cryptoDataSource.search(query)
    }
}
