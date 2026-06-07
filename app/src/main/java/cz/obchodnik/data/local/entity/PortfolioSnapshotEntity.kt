package cz.obchodnik.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "portfolio_snapshots",
    indices = [Index(value = ["dayStartMillis", "currency"], unique = true)],
)
data class PortfolioSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayStartMillis: Long,
    val totalValue: Double,
    val totalInvested: Double,
    val currency: String,
)
