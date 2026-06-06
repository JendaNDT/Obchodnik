package cz.obchodnik.ui.markets

import cz.obchodnik.domain.AssetType

enum class MarketCategory(val label: String) {
    ALL("Vše"),
    CRYPTO("Krypto"),
    METALS("Kovy"),
    COMMODITIES("Komodity"),
    INDICES("Indexy");

    fun includes(type: AssetType): Boolean =
        when (this) {
            ALL -> true
            CRYPTO -> type == AssetType.CRYPTO
            METALS -> type == AssetType.METAL
            COMMODITIES -> type == AssetType.COMMODITY
            INDICES -> type == AssetType.INDEX
        }
}
