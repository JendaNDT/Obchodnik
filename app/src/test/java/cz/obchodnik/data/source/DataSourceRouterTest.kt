package cz.obchodnik.data.source

import cz.obchodnik.core.Result
import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataSourceRouterTest {

    @Test
    fun `quotes routes assets correctly and merges results`() = runTest {
        val cryptoAsset = Asset("cg:btc", "BTC", "Bitcoin", AssetType.CRYPTO, DataProvider.COINGECKO, "btc", null, null)
        val avAsset = Asset("av:c:WTI", "WTI", "Crude WTI", AssetType.COMMODITY, DataProvider.ALPHAVANTAGE, "WTI", null, null)

        val cryptoQuote = Quote("cg:btc", 50000.0, null, null, null, null, null, null, null, emptyList(), "usd", 0L)
        val avQuote = Quote("av:c:WTI", 70.0, null, null, null, null, null, null, null, emptyList(), "usd", 0L)

        val fakeCryptoSource = FakeDataSource(quotesResult = Result.Success(listOf(cryptoQuote)))
        val fakeAvSource = FakeDataSource(quotesResult = Result.Success(listOf(avQuote)))

        val router = DataSourceRouter(fakeCryptoSource, fakeAvSource)

        val result = router.quotes(listOf(cryptoAsset, avAsset), "usd")

        assertTrue(result is Result.Success)
        val merged = (result as Result.Success).data
        assertEquals(2, merged.size)
        assertTrue(merged.any { it.assetId == "cg:btc" })
        assertTrue(merged.any { it.assetId == "av:c:WTI" })

        assertEquals(1, fakeCryptoSource.quotesCalls)
        assertEquals(1, fakeAvSource.quotesCalls)
    }

    @Test
    fun `history and candles route based on asset source`() = runTest {
        val cryptoAsset = Asset("cg:btc", "BTC", "Bitcoin", AssetType.CRYPTO, DataProvider.COINGECKO, "btc", null, null)
        val avAsset = Asset("av:c:WTI", "WTI", "Crude WTI", AssetType.COMMODITY, DataProvider.ALPHAVANTAGE, "WTI", null, null)

        val fakeCryptoSource = FakeDataSource()
        val fakeAvSource = FakeDataSource()

        val router = DataSourceRouter(fakeCryptoSource, fakeAvSource)

        router.history(cryptoAsset, ChartRange.D1, "usd")
        assertEquals(1, fakeCryptoSource.historyCalls)
        assertEquals(0, fakeAvSource.historyCalls)

        router.candles(avAsset, ChartRange.W1, "usd")
        assertEquals(0, fakeCryptoSource.candlesCalls)
        assertEquals(1, fakeAvSource.candlesCalls)
    }

    private class FakeDataSource(
        private val quotesResult: Result<List<Quote>> = Result.Success(emptyList())
    ) : MarketDataSource {
        var quotesCalls = 0
        var historyCalls = 0
        var candlesCalls = 0
        var searchCalls = 0

        override suspend fun quotes(assets: List<Asset>, currency: String): Result<List<Quote>> {
            quotesCalls++
            return quotesResult
        }

        override suspend fun history(asset: Asset, range: ChartRange, currency: String): Result<List<PricePoint>> {
            historyCalls++
            return Result.Success(emptyList())
        }

        override suspend fun candles(asset: Asset, range: ChartRange, currency: String): Result<List<Candle>> {
            candlesCalls++
            return Result.Success(emptyList())
        }

        override suspend fun search(query: String): Result<List<Asset>> {
            searchCalls++
            return Result.Success(emptyList())
        }
    }
}
