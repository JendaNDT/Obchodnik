package cz.obchodnik.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val name: String,
    val type: String,
    val provider: String,
    val sourceId: String,
    val colorHex: String?,
    val logoUrl: String?,
    val inWatchlist: Boolean,
    val sortOrder: Int,
)
