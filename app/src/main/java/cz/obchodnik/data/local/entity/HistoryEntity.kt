package cz.obchodnik.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "history",
    primaryKeys = ["assetId", "range", "currency", "kind"],
)
data class HistoryEntity(
    val assetId: String,
    val range: String,
    val currency: String,
    val kind: String,
    val pointsJson: String,
    val updatedAt: Long,
)
