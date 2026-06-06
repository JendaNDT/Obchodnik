package cz.obchodnik.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import cz.obchodnik.data.local.dao.AlertDao
import cz.obchodnik.data.local.dao.AssetDao
import cz.obchodnik.data.local.dao.HistoryDao
import cz.obchodnik.data.local.dao.HoldingDao
import cz.obchodnik.data.local.dao.QuoteDao
import cz.obchodnik.data.local.entity.AlertEntity
import cz.obchodnik.data.local.entity.AssetEntity
import cz.obchodnik.data.local.entity.HistoryEntity
import cz.obchodnik.data.local.entity.HoldingEntity
import cz.obchodnik.data.local.entity.QuoteEntity

@Database(
    entities = [
        AssetEntity::class,
        QuoteEntity::class,
        HistoryEntity::class,
        HoldingEntity::class,
        AlertEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class ObchodnikDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun quoteDao(): QuoteDao
    abstract fun historyDao(): HistoryDao
    abstract fun holdingDao(): HoldingDao
    abstract fun alertDao(): AlertDao
}
