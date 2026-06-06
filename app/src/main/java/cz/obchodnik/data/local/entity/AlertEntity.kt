package cz.obchodnik.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetId: String,
    val above: Boolean,
    val target: Double,
    val enabled: Boolean,
    val triggeredAt: Long?,
)
