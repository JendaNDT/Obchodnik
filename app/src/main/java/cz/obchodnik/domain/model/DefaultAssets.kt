package cz.obchodnik.domain.model

import cz.obchodnik.domain.AssetType

object DefaultAssets {
    val watchlist: List<Asset> =
        StaticAssetCatalog.assets.filter { it.symbol in setOf("BTC", "ETH", "SOL", "PAXG", "DOGE") }
}
