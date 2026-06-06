package cz.obchodnik.data.source

import cz.obchodnik.core.Result
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.remote.alphavantage.AlphaVantageApi
import cz.obchodnik.data.remote.alphavantage.AlphaVantageMapper
import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class CommodityDataSource(
    private val api: AlphaVantageApi,
    private val settingsRepository: SettingsRepository,
    private val currencyRateProvider: CurrencyRateProvider,
) : MarketDataSource {

    private suspend fun canMakeRequest(): Boolean {
        val settings = settingsRepository.settings.first()
        val todayStr = LocalDate.now().toString()
        val count = if (settings.avCountDate == todayStr) settings.avDailyCount else 0

        if (count >= 25) {
            return false
        }

        settingsRepository.setAlphaVantageDailyCount(count + 1, todayStr)
        return true
    }

    override suspend fun quotes(assets: List<Asset>, currency: String): Result<List<Quote>> {
        val avAssets = assets.filter { it.source == DataProvider.ALPHAVANTAGE }
        if (avAssets.isEmpty()) return Result.Success(emptyList())

        val settings = settingsRepository.settings.first()
        val apiKey = settings.alphaVantageKey
        if (apiKey.isBlank()) {
            return Result.Error("Zadejte prosím API klíč pro Alpha Vantage v Nastavení.")
        }

        return runCatching {
            val usdToCzkRate = currencyRateProvider.getUsdCzkRate()
            val now = System.currentTimeMillis()
            val results = mutableListOf<Quote>()

            for (asset in avAssets) {
                if (!canMakeRequest()) {
                    return Result.Error("Překročen denní limit 25 požadavků pro Alpha Vantage. Další aktualizace proběhne zítra.")
                }

                val quote = when (asset.type) {
                    AssetType.COMMODITY -> {
                        val dto = api.getCommodity(function = asset.sourceId, apiKey = apiKey)
                        AlphaVantageMapper.mapCommodityToQuote(dto, asset.id, currency, usdToCzkRate, now)
                    }
                    AssetType.INDEX -> {
                        val dto = api.getStockSeries(symbol = asset.sourceId, apiKey = apiKey)
                        AlphaVantageMapper.mapStockSeriesToQuote(dto, asset.id, currency, usdToCzkRate, now)
                    }
                    else -> throw IllegalArgumentException("Unsupported Alpha Vantage asset type: ${asset.type}")
                }
                results.add(quote)
            }
            results
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: "Chyba při načítání dat z Alpha Vantage", it) }
        )
    }

    override suspend fun history(asset: Asset, range: ChartRange, currency: String): Result<List<PricePoint>> {
        if (asset.source != DataProvider.ALPHAVANTAGE) {
            return Result.Error("Nepodporovaný zdroj dat pro toto aktivum.")
        }

        val settings = settingsRepository.settings.first()
        val apiKey = settings.alphaVantageKey
        if (apiKey.isBlank()) {
            return Result.Error("Zadejte prosím API klíč pro Alpha Vantage v Nastavení.")
        }

        if (!canMakeRequest()) {
            return Result.Error("Překročen denní limit 25 požadavků pro Alpha Vantage. Další aktualizace proběhne zítra.")
        }

        return runCatching {
            val usdToCzkRate = currencyRateProvider.getUsdCzkRate()
            when (asset.type) {
                AssetType.COMMODITY -> {
                    val dto = api.getCommodity(function = asset.sourceId, apiKey = apiKey)
                    AlphaVantageMapper.mapCommodityToHistory(dto, range, currency, usdToCzkRate)
                }
                AssetType.INDEX -> {
                    val dto = api.getStockSeries(symbol = asset.sourceId, apiKey = apiKey)
                    AlphaVantageMapper.mapStockSeriesToHistory(dto, range, currency, usdToCzkRate)
                }
                else -> throw IllegalArgumentException("Unsupported Alpha Vantage asset type: ${asset.type}")
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: "Chyba při načítání historie z Alpha Vantage", it) }
        )
    }

    override suspend fun candles(asset: Asset, range: ChartRange, currency: String): Result<List<Candle>> {
        if (asset.source != DataProvider.ALPHAVANTAGE) {
            return Result.Error("Nepodporovaný zdroj dat pro toto aktivum.")
        }

        val settings = settingsRepository.settings.first()
        val apiKey = settings.alphaVantageKey
        if (apiKey.isBlank()) {
            return Result.Error("Zadejte prosím API klíč pro Alpha Vantage v Nastavení.")
        }

        if (!canMakeRequest()) {
            return Result.Error("Překročen denní limit 25 požadavků pro Alpha Vantage. Další aktualizace proběhne zítra.")
        }

        return runCatching {
            val usdToCzkRate = currencyRateProvider.getUsdCzkRate()
            when (asset.type) {
                AssetType.COMMODITY -> {
                    val dto = api.getCommodity(function = asset.sourceId, apiKey = apiKey)
                    val historyPoints = AlphaVantageMapper.mapCommodityToHistory(dto, range, currency, usdToCzkRate)
                    historyPoints.map { Candle(it.timestamp, it.price, it.price, it.price, it.price) }
                }
                AssetType.INDEX -> {
                    val dto = api.getStockSeries(symbol = asset.sourceId, apiKey = apiKey)
                    AlphaVantageMapper.mapStockSeriesToCandles(dto, range, currency, usdToCzkRate)
                }
                else -> throw IllegalArgumentException("Unsupported Alpha Vantage asset type: ${asset.type}")
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: "Chyba při načítání svíček z Alpha Vantage", it) }
        )
    }

    override suspend fun search(query: String): Result<List<Asset>> {
        return Result.Success(emptyList())
    }
}
