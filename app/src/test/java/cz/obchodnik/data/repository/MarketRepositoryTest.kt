package cz.obchodnik.data.repository

import cz.obchodnik.core.Result
import cz.obchodnik.core.TimeProvider
import cz.obchodnik.data.local.AssetLocalStore
import cz.obchodnik.data.local.QuoteLocalStore
import cz.obchodnik.data.local.dao.HistoryDao
import cz.obchodnik.data.local.entity.AssetEntity
import cz.obchodnik.data.local.entity.HistoryEntity
import cz.obchodnik.data.local.entity.QuoteEntity
import cz.obchodnik.data.local.toEntity
import cz.obchodnik.data.prefs.AppSettings
import cz.obchodnik.data.prefs.SettingsStore
import cz.obchodnik.data.source.MarketDataSource
import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketRepositoryTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `fresh cached quotes skip remote refresh`() = runTest {
        val asset = bitcoin()
        val cachedQuote = quote(updatedAt = 1_000L)
        val quoteStore = FakeQuoteStore(listOf(cachedQuote.toEntity(json)))
        val source = FakeMarketDataSource(Result.Success(listOf(quote(updatedAt = 2_000L))))
        val repository = repository(
            quoteStore = quoteStore,
            source = source,
            nowMillis = 60_000L,
        )

        val result = repository.refreshQuotes(listOf(asset), currency = "usd")

        assertTrue(result is Result.Success)
        assertEquals(0, source.quoteCalls)
        assertEquals(1_000L, (result as Result.Success).data.first().updatedAt)
    }

    @Test
    fun `stale cached quotes refresh remote and update cache`() = runTest {
        val asset = bitcoin()
        val quoteStore = FakeQuoteStore(listOf(quote(updatedAt = 1_000L).toEntity(json)))
        val source = FakeMarketDataSource(Result.Success(listOf(quote(updatedAt = 500_000L, price = 62_000.0))))
        val repository = repository(
            quoteStore = quoteStore,
            source = source,
            nowMillis = 2_000_000L,
        )

        val result = repository.refreshQuotes(listOf(asset), currency = "usd")

        assertTrue(result is Result.Success)
        assertEquals(1, source.quoteCalls)
        assertEquals(62_000.0, (result as Result.Success).data.first().price, 0.0)
        assertEquals(62_000.0, quoteStore.savedQuotes.first().price, 0.0)
    }

    @Test
    fun `remote error returns cache when available`() = runTest {
        val asset = bitcoin()
        val quoteStore = FakeQuoteStore(listOf(quote(updatedAt = 1_000L).toEntity(json)))
        val source = FakeMarketDataSource(Result.Error("offline"))
        val repository = repository(
            quoteStore = quoteStore,
            source = source,
            nowMillis = 2_000_000L,
        )

        val result = repository.refreshQuotes(listOf(asset), currency = "usd")

        assertTrue(result is Result.Success)
        assertEquals(1, source.quoteCalls)
        result as Result.Success
        assertEquals(60_000.0, result.data.first().price, 0.0)
        assertEquals("Čerstvá data se nepodařilo načíst. Zobrazuji poslední uložená data.", result.notice)
    }

    @Test
    fun `partial remote success returns remote data with cached fallback and notice`() = runTest {
        val cryptoAsset = bitcoin()
        val avAsset = cryptoAsset.copy(
            id = "av:c:WTI",
            symbol = "WTI",
            name = "Ropa WTI",
            type = AssetType.COMMODITY,
            source = DataProvider.ALPHAVANTAGE,
            sourceId = "WTI",
        )
        val cachedAvQuote = quote(updatedAt = 1_000L, price = 70.0).copy(assetId = "av:c:WTI")
        val freshCryptoQuote = quote(updatedAt = 500_000L, price = 62_000.0)
        val quoteStore = FakeQuoteStore(listOf(cachedAvQuote.toEntity(json)))
        val source = FakeMarketDataSource(
            Result.Success(
                data = listOf(freshCryptoQuote),
                notice = "Překročen denní limit 25 požadavků pro Alpha Vantage.",
            ),
        )
        val repository = repository(
            quoteStore = quoteStore,
            source = source,
            nowMillis = 2_000_000L,
        )

        val result = repository.refreshQuotes(listOf(cryptoAsset, avAsset), currency = "usd")

        assertTrue(result is Result.Success)
        result as Result.Success
        assertEquals(listOf("cg:bitcoin", "av:c:WTI"), result.data.map { it.assetId })
        assertEquals(62_000.0, result.data.first { it.assetId == "cg:bitcoin" }.price, 0.0)
        assertEquals(70.0, result.data.first { it.assetId == "av:c:WTI" }.price, 0.0)
        assertEquals("Alpha Vantage limit je dnes vyčerpaný. Zobrazuji poslední uložená data.", result.notice)
    }

    private fun repository(
        quoteStore: FakeQuoteStore,
        source: FakeMarketDataSource,
        nowMillis: Long,
    ): MarketRepository =
        MarketRepository(
            assetStore = FakeAssetStore(),
            quoteStore = quoteStore,
            historyDao = FakeHistoryDao(),
            marketDataSource = source,
            settingsStore = FakeSettingsStore(AppSettings(refreshIntervalMinutes = 30)),
            json = json,
            timeProvider = object : TimeProvider {
                override fun nowMillis(): Long = nowMillis
            },
        )

    private fun bitcoin(): Asset =
        Asset(
            id = "cg:bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            type = AssetType.CRYPTO,
            source = DataProvider.COINGECKO,
            sourceId = "bitcoin",
            colorHex = "#f7931a",
            logoUrl = null,
        )

    private fun quote(updatedAt: Long, price: Double = 60_000.0): Quote =
        Quote(
            assetId = "cg:bitcoin",
            price = price,
            change24hPct = 1.0,
            change7dPct = null,
            change30dPct = null,
            high24h = null,
            low24h = null,
            marketCap = null,
            volume24h = null,
            sparkline7d = listOf(price),
            currency = "usd",
            updatedAt = updatedAt,
        )
}

private class FakeHistoryDao : HistoryDao {
    override suspend fun upsertHistory(history: HistoryEntity) = Unit
    override suspend fun history(assetId: String, range: String, currency: String, kind: String): HistoryEntity? = null
}

private class FakeSettingsStore(settings: AppSettings) : SettingsStore {
    override val settings: Flow<AppSettings> = flowOf(settings)
}

private class FakeMarketDataSource(
    private val quotesResult: Result<List<Quote>>,
) : MarketDataSource {
    var quoteCalls = 0

    override suspend fun quotes(assets: List<Asset>, currency: String): Result<List<Quote>> {
        quoteCalls += 1
        return quotesResult
    }

    override suspend fun history(asset: Asset, range: ChartRange, currency: String): Result<List<PricePoint>> =
        Result.Success(emptyList())

    override suspend fun candles(asset: Asset, range: ChartRange, currency: String): Result<List<Candle>> =
        Result.Success(emptyList())

    override suspend fun search(query: String): Result<List<Asset>> =
        Result.Success(emptyList())
}

private class FakeQuoteStore(
    initialQuotes: List<QuoteEntity> = emptyList(),
) : QuoteLocalStore {
    val savedQuotes = initialQuotes.toMutableList()

    override suspend fun upsertQuotes(quotes: List<QuoteEntity>) {
        quotes.forEach { quote ->
            savedQuotes.removeAll { it.assetId == quote.assetId && it.currency == quote.currency }
            savedQuotes += quote
        }
    }

    override fun observeQuotes(currency: String): Flow<List<QuoteEntity>> =
        flowOf(savedQuotes.filter { it.currency == currency })

    override suspend fun quotesForAssets(assetIds: List<String>, currency: String): List<QuoteEntity> =
        savedQuotes.filter { it.currency == currency && it.assetId in assetIds }
}

class FakeAssetStore : AssetLocalStore {
    val assets = mutableListOf<AssetEntity>()

    override suspend fun upsertAssets(assets: List<AssetEntity>) {
        assets.forEach { upsertAsset(it) }
    }

    override suspend fun upsertAsset(asset: AssetEntity) {
        assets.removeAll { it.id == asset.id }
        assets += asset
    }

    override suspend fun assetById(id: String): AssetEntity? =
        assets.firstOrNull { it.id == id }

    override fun observeWatchlistAssets(): Flow<List<AssetEntity>> =
        flowOf(assets.filter { it.inWatchlist }.sortedBy { it.sortOrder })

    override suspend fun watchlistAssets(): List<AssetEntity> =
        assets.filter { it.inWatchlist }.sortedBy { it.sortOrder }

    override suspend fun maxSortOrder(): Int? =
        assets.filter { it.inWatchlist }.maxOfOrNull { it.sortOrder }

    override suspend fun setWatchlistState(id: String, inWatchlist: Boolean, sortOrder: Int) {
        val index = assets.indexOfFirst { it.id == id }
        if (index >= 0) {
            assets[index] = assets[index].copy(inWatchlist = inWatchlist, sortOrder = sortOrder)
        }
    }
}
