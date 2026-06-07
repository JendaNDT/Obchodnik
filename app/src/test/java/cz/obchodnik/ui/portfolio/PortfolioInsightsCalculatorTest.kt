package cz.obchodnik.ui.portfolio

import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.Holding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortfolioInsightsCalculatorTest {

    @Test
    fun `calculate returns empty insights for empty portfolio`() {
        val insights = PortfolioInsightsCalculator.calculate(emptyList(), totalValue = 0.0)

        assertEquals(0, insights.positionCount)
        assertNull(insights.largestPosition)
        assertNull(insights.largestPositionSharePct)
        assertNull(insights.bestPerformer)
        assertNull(insights.worstPerformer)
    }

    @Test
    fun `calculate finds largest position and performers`() {
        val btc = item("BTC", value = 6_000.0, invested = 5_000.0, plValue = 1_000.0)
        val eth = item("ETH", value = 3_000.0, invested = 4_000.0, plValue = -1_000.0)
        val sol = item("SOL", value = 1_000.0, invested = 800.0, plValue = 200.0)

        val insights = PortfolioInsightsCalculator.calculate(
            items = listOf(btc, eth, sol),
            totalValue = 10_000.0,
        )

        assertEquals(3, insights.positionCount)
        assertEquals("BTC", insights.largestPosition?.asset?.symbol)
        assertEquals(60.0, insights.largestPositionSharePct ?: 0.0, 0.01)
        assertEquals("BTC", insights.bestPerformer?.asset?.symbol)
        assertEquals("ETH", insights.worstPerformer?.asset?.symbol)
    }

    private fun item(
        symbol: String,
        value: Double,
        invested: Double,
        plValue: Double,
    ): PortfolioItem = PortfolioItem(
        holding = Holding(id = symbol.hashCode().toLong(), assetId = symbol, qty = 1.0, avgPrice = invested),
        asset = Asset(
            id = symbol,
            symbol = symbol,
            name = symbol,
            type = AssetType.CRYPTO,
            source = DataProvider.COINGECKO,
            sourceId = symbol.lowercase(),
            colorHex = null,
            logoUrl = null,
        ),
        quote = null,
        value = value,
        invested = invested,
        plValue = plValue,
        plPct = if (invested > 0.0) (plValue / invested) * 100.0 else null,
    )
}
