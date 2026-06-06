package cz.obchodnik.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "quotes",
    primaryKeys = ["assetId", "currency"],
)
data class QuoteEntity(
    val assetId: String,
    val currency: String,
    val price: Double,
    val change24h: Double?,
    val change7d: Double?,
    val change30d: Double?,
    val high24h: Double?,
    val low24h: Double?,
    val marketCap: Double?,
    val volume24h: Double?,
    val sparklineJson: String,
    val updatedAt: Long,
)
