package cz.obchodnik.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PricePoint(
    val timestamp: Long,
    val price: Double,
)
