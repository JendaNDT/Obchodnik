package cz.obchodnik.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = true,
)
abstract class ObchodnikDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun quoteDao(): QuoteDao
    abstract fun historyDao(): HistoryDao
    abstract fun holdingDao(): HoldingDao
    abstract fun alertDao(): AlertDao

    companion object {
        /**
         * Schema migrations applied on upgrade. When an entity changes, bump the
         * @Database version, add the corresponding Migration here and commit the
         * newly generated schemas/<version>.json. Do not use destructive
         * migration for upgrades once the app ships real user data.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alerts ADD COLUMN triggeredPrice REAL")
                db.execSQL("ALTER TABLE alerts ADD COLUMN triggeredCurrency TEXT")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_2_3)
    }
}
