package cz.obchodnik.domain.model

import cz.obchodnik.domain.AssetType

data class Asset(
    val id: String,
    val symbol: String,
    val name: String,
    val type: AssetType,
    val source: DataProvider,
    val sourceId: String,
    val colorHex: String?,
    val logoUrl: String?,
)
