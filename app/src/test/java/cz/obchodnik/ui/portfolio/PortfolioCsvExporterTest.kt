package cz.obchodnik.ui.portfolio

import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.Holding
import cz.obchodnik.domain.model.Quote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioCsvExporterTest {

    @Test
    fun `export writes header and portfolio rows`() {
        val state = PortfolioUiState(
            items = listOf(
                PortfolioItem(
                    holding = Holding(id = 7, assetId = "cg:bitcoin", qty = 0.125, avgPrice = 50_000.0),
                    asset = Asset(
                        id = "cg:bitcoin",
                        symbol = "BTC",
                        name = "Bitcoin",
                        type = AssetType.CRYPTO,
                        source = DataProvider.COINGECKO,
                        sourceId = "bitcoin",
                        colorHex = null,
                        logoUrl = null,
                    ),
                    quote = Quote(
                        assetId = "cg:bitcoin",
                        price = 60_000.0,
                        change24hPct = 1.0,
                        change7dPct = null,
                        change30dPct = null,
                        high24h = null,
                        low24h = null,
                        marketCap = null,
                        volume24h = null,
                        sparkline7d = emptyList(),
                        updatedAt = 1_700_000_000_000L,
                        currency = "usd",
                    ),
                    value = 7_500.0,
                    invested = 6_250.0,
                    plValue = 1_250.0,
                    plPct = 20.0,
                ),
            ),
            currency = "usd",
        )

        val csv = PortfolioCsvExporter.export(state)

        assertTrue(csv.startsWith("Symbol,Název,Množství,Nákupní cena,Aktuální cena,Hodnota,Investováno,P/L,P/L %,Měna\n"))
        assertTrue(csv.contains("BTC,Bitcoin,0.125,50000,60000,7500,6250,1250,20,USD\n"))
    }

    @Test
    fun `export escapes csv cells`() {
        val state = PortfolioUiState(
            items = listOf(
                PortfolioItem(
                    holding = Holding(id = 1, assetId = "custom", qty = 1.0, avgPrice = 10.0),
                    asset = Asset(
                        id = "custom",
                        symbol = "ACME",
                        name = "ACME, \"Growth\"",
                        type = AssetType.INDEX,
                        source = DataProvider.ALPHAVANTAGE,
                        sourceId = "ACME",
                        colorHex = null,
                        logoUrl = null,
                    ),
                    quote = null,
                    value = 10.0,
                    invested = 10.0,
                    plValue = 0.0,
                    plPct = null,
                ),
            ),
            currency = "czk",
        )

        val lines = PortfolioCsvExporter.export(state).lines()

        assertEquals("ACME,\"ACME, \"\"Growth\"\"\",1,10,10,10,10,0,,CZK", lines[1])
    }
}
