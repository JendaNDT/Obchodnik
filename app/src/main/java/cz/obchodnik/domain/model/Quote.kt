package cz.obchodnik.domain.model

data class Quote(
    val assetId: String,
    val price: Double,
    val change24hPct: Double?,
    val change7dPct: Double?,
    val change30dPct: Double?,
    val high24h: Double?,
    val low24h: Double?,
    val marketCap: Double?,
    val volume24h: Double?,
    val sparkline7d: List<Double>,
    val currency: String,
    val updatedAt: Long,
)
