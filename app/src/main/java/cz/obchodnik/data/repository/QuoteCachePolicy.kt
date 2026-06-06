package cz.obchodnik.data.repository

import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Quote

class QuoteCachePolicy {
    fun shouldRefreshQuotes(
        assets: List<Asset>,
        cachedQuotes: List<Quote>,
        nowMillis: Long,
        ttlMillis: Long,
        force: Boolean = false,
    ): Boolean {
        if (force) return true
        if (assets.isEmpty()) return false
        if (cachedQuotes.isEmpty()) return true

        val cachedByAsset = cachedQuotes.associateBy { it.assetId }
        return assets.any { asset ->
            val quote = cachedByAsset[asset.id] ?: return@any true
            nowMillis - quote.updatedAt >= ttlMillis
        }
    }
}
