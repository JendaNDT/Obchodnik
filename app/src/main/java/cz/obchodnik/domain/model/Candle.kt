package cz.obchodnik.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
)
