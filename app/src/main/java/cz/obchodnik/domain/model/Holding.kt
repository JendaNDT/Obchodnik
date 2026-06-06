package cz.obchodnik.domain.model

data class Holding(
    val id: Long,
    val assetId: String,
    val qty: Double,
    val avgPrice: Double,
)
