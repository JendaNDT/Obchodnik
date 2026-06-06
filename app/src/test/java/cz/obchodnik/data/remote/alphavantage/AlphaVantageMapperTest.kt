package cz.obchodnik.data.remote.alphavantage

import cz.obchodnik.data.remote.alphavantage.dto.AlphaVantageCommodityDto
import cz.obchodnik.data.remote.alphavantage.dto.AlphaVantageSeriesDto
import cz.obchodnik.domain.model.ChartRange
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AlphaVantageMapperTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `commodity DTO maps to Quote in USD and CZK`() {
        val commodityDto = json.decodeFromString<AlphaVantageCommodityDto>(
            readFixture("alphavantage/commodity.json")
        )

        val usdQuote = AlphaVantageMapper.mapCommodityToQuote(
            dto = commodityDto,
            assetId = "av:c:WTI",
            currency = "usd",
            usdToCzkRate = 22.5,
            nowMillis = 1000L
        )

        assertEquals("av:c:WTI", usdQuote.assetId)
        assertEquals(72.34, usdQuote.price, 0.0)
        assertEquals("usd", usdQuote.currency)
        // 24h change: ((72.34 - 71.20) / 71.20) * 100.0 = 1.6011%
        assertEquals(1.6011, usdQuote.change24hPct ?: 0.0, 0.0001)
        // Sparkline takes last 7 (we only have 3 valid ones: 72.34, 71.20, 70.50), reversed to chronological: 70.50, 71.20, 72.34
        assertEquals(3, usdQuote.sparkline7d.size)
        assertEquals(70.50, usdQuote.sparkline7d[0], 0.0)
        assertEquals(71.20, usdQuote.sparkline7d[1], 0.0)
        assertEquals(72.34, usdQuote.sparkline7d[2], 0.0)

        val czkQuote = AlphaVantageMapper.mapCommodityToQuote(
            dto = commodityDto,
            assetId = "av:c:WTI",
            currency = "czk",
            usdToCzkRate = 22.5,
            nowMillis = 1000L
        )

        assertEquals(72.34 * 22.5, czkQuote.price, 0.0)
        assertEquals("czk", czkQuote.currency)
        assertEquals(1.6011, czkQuote.change24hPct ?: 0.0, 0.0001) // Percent change is currency-invariant
        assertEquals(70.50 * 22.5, czkQuote.sparkline7d[0], 0.0)
    }

    @Test
    fun `commodity DTO mapping checks API limit note`() {
        val noteDto = AlphaVantageCommodityDto(note = "Thank you for using Alpha Vantage...")
        try {
            AlphaVantageMapper.mapCommodityToQuote(
                dto = noteDto,
                assetId = "av:c:WTI",
                currency = "usd",
                usdToCzkRate = 22.5,
                nowMillis = 1000L
            )
            fail("Should have thrown IllegalStateException for Note")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Alpha Vantage API Note"))
        }
    }

    @Test
    fun `stock series DTO maps to Quote`() {
        val seriesDto = json.decodeFromString<AlphaVantageSeriesDto>(
            readFixture("alphavantage/stock_series.json")
        )

        val quote = AlphaVantageMapper.mapStockSeriesToQuote(
            dto = seriesDto,
            assetId = "av:i:SPY",
            currency = "usd",
            usdToCzkRate = 23.0,
            nowMillis = 12345L
        )

        assertEquals("av:i:SPY", quote.assetId)
        assertEquals(529.12, quote.price, 0.0)
        // 24h change: ((529.12 - 527.92) / 527.92) * 100.0 = 0.2273%
        assertEquals(0.2273, quote.change24hPct ?: 0.0, 0.0001)
        assertEquals(530.0, quote.high24h ?: 0.0, 0.0)
        assertEquals(528.0, quote.low24h ?: 0.0, 0.0)
        assertEquals(1234567.0, quote.volume24h ?: 0.0, 0.0)
        assertEquals("usd", quote.currency)

        // Sparkline chronological: 525.0, 527.92, 529.12
        assertEquals(3, quote.sparkline7d.size)
        assertEquals(525.0, quote.sparkline7d[0], 0.0)
        assertEquals(527.92, quote.sparkline7d[1], 0.0)
        assertEquals(529.12, quote.sparkline7d[2], 0.0)
    }

    @Test
    fun `stock series DTO maps to history and candles with range filtering`() {
        val seriesDto = json.decodeFromString<AlphaVantageSeriesDto>(
            readFixture("alphavantage/stock_series.json")
        )

        // Date of newest bar in fixture is 2026-06-05 (which is 1780617600000 ms in UTC)
        val historyW1 = AlphaVantageMapper.mapStockSeriesToHistory(
            dto = seriesDto,
            range = ChartRange.W1,
            currency = "usd",
            usdToCzkRate = 23.0
        )

        // In chronological order
        assertEquals(3, historyW1.size)
        assertEquals(525.0, historyW1[0].price, 0.0)

        val candlesAll = AlphaVantageMapper.mapStockSeriesToCandles(
            dto = seriesDto,
            range = ChartRange.ALL,
            currency = "usd",
            usdToCzkRate = 23.0
        )

        assertEquals(3, candlesAll.size)
        assertEquals(524.0, candlesAll[0].open, 0.0)
        assertEquals(526.0, candlesAll[0].high, 0.0)
        assertEquals(523.0, candlesAll[0].low, 0.0)
        assertEquals(525.0, candlesAll[0].close, 0.0)
    }

    private fun readFixture(path: String): String =
        checkNotNull(javaClass.classLoader?.getResource(path)) {
            "Missing test fixture: $path"
        }.readText()
}
