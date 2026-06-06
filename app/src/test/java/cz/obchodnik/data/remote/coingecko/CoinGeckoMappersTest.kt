package cz.obchodnik.data.remote.coingecko

import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoMarketChartDto
import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoMarketCoinDto
import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoSearchResponseDto
import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.DataProvider
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinGeckoMappersTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `market coin maps to asset and quote`() {
        val markets = json.decodeFromString<List<CoinGeckoMarketCoinDto>>(
            readFixture("coingecko/markets.json"),
        )

        val bitcoin = markets.first()
        val asset = bitcoin.toAsset()
        val quote = bitcoin.toQuote(currency = "usd", fallbackUpdatedAt = 0L)

        assertEquals("cg:bitcoin", asset.id)
        assertEquals("BTC", asset.symbol)
        assertEquals("Bitcoin", asset.name)
        assertEquals(AssetType.CRYPTO, asset.type)
        assertEquals(DataProvider.COINGECKO, asset.source)
        assertEquals("bitcoin", asset.sourceId)
        assertEquals("#f7931a", asset.colorHex)

        assertEquals("cg:bitcoin", quote.assetId)
        assertEquals(60729.0, quote.price, 0.0)
        assertEquals(-0.14447, quote.change24hPct ?: 0.0, 0.00001)
        assertEquals(-3.10, quote.change7dPct ?: 0.0, 0.00001)
        assertEquals(18.40, quote.change30dPct ?: 0.0, 0.00001)
        assertEquals(61876.0, quote.high24h ?: 0.0, 0.0)
        assertEquals(59228.0, quote.low24h ?: 0.0, 0.0)
        assertEquals(3, quote.sparkline7d.size)
        assertEquals("usd", quote.currency)
        assertTrue(quote.updatedAt > 0L)
    }

    @Test
    fun `tokenized metal maps to metal asset`() {
        val markets = json.decodeFromString<List<CoinGeckoMarketCoinDto>>(
            readFixture("coingecko/markets.json"),
        )

        val asset = markets.last().toAsset()

        assertEquals("cg:pax-gold", asset.id)
        assertEquals("PAXG", asset.symbol)
        assertEquals(AssetType.METAL, asset.type)
        assertEquals("#d4af37", asset.colorHex)
    }

    @Test
    fun `market chart maps prices to points`() {
        val chart = json.decodeFromString<CoinGeckoMarketChartDto>(
            readFixture("coingecko/market_chart.json"),
        )

        val points = chart.toPricePoints()

        assertEquals(2, points.size)
        assertEquals(1780680896625L, points.first().timestamp)
        assertEquals(60777.015709762985, points.first().price, 0.0)
    }

    @Test
    fun `ohlc rows map to candles`() {
        val rows = json.decodeFromString<List<List<Double>>>(
            readFixture("coingecko/ohlc.json"),
        )

        val candles = rows.toCandles()

        assertEquals(2, candles.size)
        assertEquals(1780680600000L, candles.first().timestamp)
        assertEquals(61297.0, candles.first().open, 0.0)
        assertEquals(61326.0, candles.first().high, 0.0)
        assertEquals(60694.0, candles.first().low, 0.0)
        assertEquals(60694.0, candles.first().close, 0.0)
    }

    @Test
    fun `search response maps coin to asset`() {
        val response = json.decodeFromString<CoinGeckoSearchResponseDto>(
            readFixture("coingecko/search.json"),
        )

        val asset = response.coins.first().toAsset()

        assertEquals("cg:bitcoin", asset.id)
        assertEquals("BTC", asset.symbol)
        assertEquals("Bitcoin", asset.name)
        assertEquals(DataProvider.COINGECKO, asset.source)
    }

    private fun readFixture(path: String): String =
        checkNotNull(javaClass.classLoader?.getResource(path)) {
            "Missing test fixture: $path"
        }.readText()
}
