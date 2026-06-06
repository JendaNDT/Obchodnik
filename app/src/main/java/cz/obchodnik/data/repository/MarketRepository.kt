package cz.obchodnik.data.repository

import cz.obchodnik.core.Result
import cz.obchodnik.core.SystemTimeProvider
import cz.obchodnik.core.TimeProvider
import cz.obchodnik.data.local.AssetLocalStore
import cz.obchodnik.data.local.dao.HistoryDao
import cz.obchodnik.data.local.QuoteLocalStore
import cz.obchodnik.data.local.candlesToEntity
import cz.obchodnik.data.local.pricePointsToEntity
import cz.obchodnik.data.local.toDomain
import cz.obchodnik.data.local.toEntity
import cz.obchodnik.data.local.toCandles
import cz.obchodnik.data.local.toPricePoints
import cz.obchodnik.data.prefs.SettingsStore
import cz.obchodnik.data.source.MarketDataSource
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.math.max

class MarketRepository(
    private val assetStore: AssetLocalStore,
    private val quoteStore: QuoteLocalStore,
    private val historyDao: HistoryDao,
    private val marketDataSource: MarketDataSource,
    private val settingsStore: SettingsStore,
    private val json: Json,
    private val timeProvider: TimeProvider = SystemTimeProvider,
    private val cachePolicy: QuoteCachePolicy = QuoteCachePolicy(),
) {
    fun observeQuotes(currency: String): Flow<List<Quote>> =
        quoteStore.observeQuotes(currency.lowercase()).map { quotes ->
            quotes.map { it.toDomain(json) }
        }

    suspend fun cachedQuote(assetId: String, currency: String): Quote? =
        quoteStore.quotesForAssets(listOf(assetId), currency.lowercase())
            .firstOrNull()
            ?.toDomain(json)

    suspend fun refreshQuotes(
        assets: List<Asset>,
        currency: String,
        force: Boolean = false,
    ): Result<List<Quote>> {
        if (assets.isEmpty()) return Result.Success(emptyList())

        val normalizedCurrency = currency.lowercase()
        val cached = quoteStore.quotesForAssets(
            assetIds = assets.map { it.id },
            currency = normalizedCurrency,
        ).map { it.toDomain(json) }
        val refreshInterval = settingsStore.settings.first().refreshIntervalMinutes
        val ttlMillis = max(1, refreshInterval).toLong() * 60_000L

        val shouldRefresh = cachePolicy.shouldRefreshQuotes(
            assets = assets,
            cachedQuotes = cached,
            nowMillis = timeProvider.nowMillis(),
            ttlMillis = ttlMillis,
            force = force,
        )
        if (!shouldRefresh) return Result.Success(cached)

        return when (val remote = marketDataSource.quotes(assets, normalizedCurrency)) {
            is Result.Success -> {
                quoteStore.upsertQuotes(remote.data.map { it.toEntity(json) })
                Result.Success(remote.data)
            }
            is Result.Error -> {
                if (cached.isNotEmpty()) Result.Success(cached) else remote
            }
            Result.Loading -> Result.Loading
        }
    }

    suspend fun history(
        asset: Asset,
        range: ChartRange,
        currency: String,
        force: Boolean = false,
    ): Result<List<PricePoint>> {
        val normalizedCurrency = currency.lowercase()
        val cached = historyDao.history(asset.id, range.name, normalizedCurrency, "line")
        if (!force && cached != null && !isHistoryStale(cached.updatedAt, range)) {
            return Result.Success(cached.toPricePoints(json))
        }
        return when (val remote = marketDataSource.history(asset, range, normalizedCurrency)) {
            is Result.Success -> {
                historyDao.upsertHistory(
                    pricePointsToEntity(
                        assetId = asset.id,
                        range = range,
                        currency = normalizedCurrency,
                        points = remote.data,
                        updatedAt = timeProvider.nowMillis(),
                        json = json,
                    ),
                )
                Result.Success(remote.data)
            }
            is Result.Error -> {
                val cachedPoints = cached?.toPricePoints(json).orEmpty()
                if (cachedPoints.isNotEmpty()) Result.Success(cachedPoints) else remote
            }
            Result.Loading -> Result.Loading
        }
    }

    suspend fun candles(
        asset: Asset,
        range: ChartRange,
        currency: String,
        force: Boolean = false,
    ): Result<List<Candle>> {
        val normalizedCurrency = currency.lowercase()
        val cached = historyDao.history(asset.id, range.name, normalizedCurrency, "candle")
        if (!force && cached != null && !isHistoryStale(cached.updatedAt, range)) {
            return Result.Success(cached.toCandles(json))
        }
        return when (val remote = marketDataSource.candles(asset, range, normalizedCurrency)) {
            is Result.Success -> {
                historyDao.upsertHistory(
                    candlesToEntity(
                        assetId = asset.id,
                        range = range,
                        currency = normalizedCurrency,
                        candles = remote.data,
                        updatedAt = timeProvider.nowMillis(),
                        json = json,
                    ),
                )
                Result.Success(remote.data)
            }
            is Result.Error -> {
                val cachedCandles = cached?.toCandles(json).orEmpty()
                if (cachedCandles.isNotEmpty()) Result.Success(cachedCandles) else remote
            }
            Result.Loading -> Result.Loading
        }
    }

    private fun isHistoryStale(updatedAt: Long, range: ChartRange): Boolean {
        val ttlMillis = when (range) {
            ChartRange.D1 -> 5 * 60_000L
            ChartRange.W1 -> 30 * 60_000L
            ChartRange.M1, ChartRange.Y1, ChartRange.ALL -> 6 * 60 * 60_000L
        }
        return timeProvider.nowMillis() - updatedAt >= ttlMillis
    }
}
