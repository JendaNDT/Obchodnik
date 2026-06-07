package cz.obchodnik.ui.markets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedMarketViewSerializerTest {

    @Test
    fun `encode then decode round trips views`() {
        val views = listOf(
            SavedMarketView("a", "Krypto růst", MarketCategory.CRYPTO, MarketSortMode.GAINERS),
            SavedMarketView("b", "Hledání BTC", MarketCategory.ALL, MarketSortMode.NAME, query = "btc"),
        )

        val decoded = SavedMarketViewSerializer.decode(SavedMarketViewSerializer.encode(views))

        assertEquals(views, decoded)
    }

    @Test
    fun `blank input decodes to empty list`() {
        assertTrue(SavedMarketViewSerializer.decode("").isEmpty())
        assertTrue(SavedMarketViewSerializer.decode("   ").isEmpty())
    }

    @Test
    fun `malformed json decodes to empty list`() {
        assertTrue(SavedMarketViewSerializer.decode("{ not json ]").isEmpty())
    }

    @Test
    fun `entries with unknown enum names are skipped`() {
        val raw = """
            [
              {"id":"ok","name":"Platný","category":"CRYPTO","sortMode":"GAINERS","query":""},
              {"id":"badCat","name":"Špatná kategorie","category":"NEZNAMA","sortMode":"NAME","query":""},
              {"id":"badSort","name":"Špatné řazení","category":"ALL","sortMode":"NEZNAME","query":""}
            ]
        """.trimIndent()

        val decoded = SavedMarketViewSerializer.decode(raw)

        assertEquals(1, decoded.size)
        assertEquals("ok", decoded.first().id)
    }
}
