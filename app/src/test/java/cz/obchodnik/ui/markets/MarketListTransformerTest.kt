package cz.obchodnik.ui.markets

import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.Quote
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketListTransformerTest {

    @Test
    fun `transform filters by query and category`() {
        val rows = MarketListTransformer.transform(
            watchlist = sampleAssets,
            quotes = sampleQuotes,
            category = MarketCategory.CRYPTO,
            query = "bit",
            sortMode = MarketSortMode.MANUAL,
        )

        assertEquals(listOf("BTC"), rows.map { it.asset.symbol })
    }

    @Test
    fun `transform sorts by biggest gainers`() {
        val rows = MarketListTransformer.transform(
            watchlist = sampleAssets,
            quotes = sampleQuotes,
            category = MarketCategory.ALL,
            query = "",
            sortMode = MarketSortMode.GAINERS,
        )

        assertEquals(listOf("ETH", "BTC", "SPY"), rows.map { it.asset.symbol })
    }

    @Test
    fun `transform sorts by price descending`() {
        val rows = MarketListTransformer.transform(
            watchlist = sampleAssets,
            quotes = sampleQuotes,
            category = MarketCategory.ALL,
            query = "",
            sortMode = MarketSortMode.PRICE,
        )

        assertEquals(listOf("BTC", "ETH", "SPY"), rows.map { it.asset.symbol })
    }

    private val sampleAssets = listOf(
        asset("cg:bitcoin", "BTC", "Bitcoin", AssetType.CRYPTO, DataProvider.COINGECKO),
        asset("cg:ethereum", "ETH", "Ethereum", AssetType.CRYPTO, DataProvider.COINGECKO),
        asset("av:i:SPY", "SPY", "S&P 500 přes ETF", AssetType.INDEX, DataProvider.ALPHAVANTAGE),
    )

    private val sampleQuotes = listOf(
        quote("cg:bitcoin", price = 60_000.0, change = 2.0),
        quote("cg:ethereum", price = 3_000.0, change = 5.0),
        quote("av:i:SPY", price = 500.0, change = -1.0),
    )

    private fun asset(
        id: String,
        symbol: String,
        name: String,
        type: AssetType,
        provider: DataProvider,
    ): Asset = Asset(
        id = id,
        symbol = symbol,
        name = name,
        type = type,
        source = provider,
        sourceId = id.substringAfterLast(':'),
        colorHex = null,
        logoUrl = null,
    )

    private fun quote(assetId: String, price: Double, change: Double): Quote = Quote(
        assetId = assetId,
        price = price,
        change24hPct = change,
        change7dPct = null,
        change30dPct = null,
        high24h = null,
        low24h = null,
        marketCap = null,
        volume24h = null,
        sparkline7d = emptyList(),
        updatedAt = 1_700_000_000_000L,
        currency = "usd",
    )
}
