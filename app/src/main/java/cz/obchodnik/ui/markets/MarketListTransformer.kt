package cz.obchodnik.ui.markets

import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Quote

object MarketListTransformer {
    fun transform(
        watchlist: List<Asset>,
        quotes: List<Quote>,
        category: MarketCategory,
        query: String,
        sortMode: MarketSortMode,
    ): List<MarketAssetUi> {
        val quotesByAsset = quotes.associateBy { it.assetId }
        val normalizedQuery = query.trim()
        return watchlist
            .filter { category.includes(it.type) }
            .filter { asset ->
                normalizedQuery.isBlank() ||
                    asset.symbol.contains(normalizedQuery, ignoreCase = true) ||
                    asset.name.contains(normalizedQuery, ignoreCase = true)
            }
            .map { asset -> MarketAssetUi(asset = asset, quote = quotesByAsset[asset.id]) }
            .let { rows ->
                when (sortMode) {
                    MarketSortMode.MANUAL -> rows
                    MarketSortMode.GAINERS -> rows.sortedByDescending { it.quote?.change24hPct ?: Double.NEGATIVE_INFINITY }
                    MarketSortMode.LOSERS -> rows.sortedBy { it.quote?.change24hPct ?: Double.POSITIVE_INFINITY }
                    MarketSortMode.NAME -> rows.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.asset.symbol })
                    MarketSortMode.PRICE -> rows.sortedByDescending { it.quote?.price ?: Double.NEGATIVE_INFINITY }
                }
            }
    }
}
