package cz.obchodnik.domain.model

data class PriceAlert(
    val id: Long,
    val assetId: String,
    val above: Boolean,
    val target: Double,
    val enabled: Boolean,
    val triggeredAt: Long?,
)
