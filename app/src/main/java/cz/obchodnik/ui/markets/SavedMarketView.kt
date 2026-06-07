package cz.obchodnik.ui.markets

/**
 * User-defined market view: a named combination of category, sort mode and search
 * query. Behaves like a [MarketQuickView] the user creates and persists.
 */
data class SavedMarketView(
    val id: String,
    val name: String,
    val category: MarketCategory,
    val sortMode: MarketSortMode,
    val query: String = "",
)
