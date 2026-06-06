package cz.obchodnik.ui.markets

import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Quote

data class MarketsUiState(
    val assets: List<MarketAssetUi> = emptyList(),
    val selectedCategory: MarketCategory = MarketCategory.ALL,
    val currency: String = "usd",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

data class MarketAssetUi(
    val asset: Asset,
    val quote: Quote?,
)
