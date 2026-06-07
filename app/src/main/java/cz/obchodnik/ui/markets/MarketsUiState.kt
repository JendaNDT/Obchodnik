package cz.obchodnik.ui.markets

import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Quote

data class MarketsUiState(
    val assets: List<MarketAssetUi> = emptyList(),
    val selectedCategory: MarketCategory = MarketCategory.ALL,
    val query: String = "",
    val sortMode: MarketSortMode = MarketSortMode.MANUAL,
    val activeQuickView: MarketQuickView? = MarketQuickView.MANUAL,
    val savedViews: List<SavedMarketView> = emptyList(),
    val currency: String = "usd",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
)

data class MarketAssetUi(
    val asset: Asset,
    val quote: Quote?,
)

enum class MarketSortMode(val label: String) {
    MANUAL("Ručně"),
    GAINERS("Růst"),
    LOSERS("Pokles"),
    NAME("Název"),
    PRICE("Cena"),
}
