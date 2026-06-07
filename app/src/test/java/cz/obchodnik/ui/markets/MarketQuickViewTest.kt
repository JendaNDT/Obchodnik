package cz.obchodnik.ui.markets

import org.junit.Assert.assertEquals
import org.junit.Test

class MarketQuickViewTest {

    @Test
    fun `manual view resets to all assets and manual ordering`() {
        assertEquals(MarketCategory.ALL, MarketQuickView.MANUAL.category)
        assertEquals(MarketSortMode.MANUAL, MarketQuickView.MANUAL.sortMode)
        assertEquals("", MarketQuickView.MANUAL.query)
    }

    @Test
    fun `gainers and losers views sort all assets`() {
        assertEquals(MarketCategory.ALL, MarketQuickView.GAINERS.category)
        assertEquals(MarketSortMode.GAINERS, MarketQuickView.GAINERS.sortMode)
        assertEquals(MarketCategory.ALL, MarketQuickView.LOSERS.category)
        assertEquals(MarketSortMode.LOSERS, MarketQuickView.LOSERS.sortMode)
    }

    @Test
    fun `asset class views keep manual ordering within category`() {
        assertEquals(MarketCategory.CRYPTO, MarketQuickView.CRYPTO.category)
        assertEquals(MarketSortMode.MANUAL, MarketQuickView.CRYPTO.sortMode)
        assertEquals(MarketCategory.INDICES, MarketQuickView.INDICES.category)
        assertEquals(MarketSortMode.MANUAL, MarketQuickView.INDICES.sortMode)
    }
}
