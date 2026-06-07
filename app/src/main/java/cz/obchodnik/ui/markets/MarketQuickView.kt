package cz.obchodnik.ui.markets

enum class MarketQuickView(
    val label: String,
    val category: MarketCategory,
    val sortMode: MarketSortMode,
    val query: String = "",
) {
    MANUAL("Ruční", MarketCategory.ALL, MarketSortMode.MANUAL),
    GAINERS("Roste", MarketCategory.ALL, MarketSortMode.GAINERS),
    LOSERS("Padá", MarketCategory.ALL, MarketSortMode.LOSERS),
    CRYPTO("Krypto", MarketCategory.CRYPTO, MarketSortMode.MANUAL),
    INDICES("Indexy", MarketCategory.INDICES, MarketSortMode.MANUAL),
}
