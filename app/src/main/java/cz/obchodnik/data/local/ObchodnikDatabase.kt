package cz.obchodnik.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.obchodnik.data.local.dao.AlertDao
import cz.obchodnik.data.local.dao.AssetDao
import cz.obchodnik.data.local.dao.HistoryDao
import cz.obchodnik.data.local.dao.HoldingDao
import cz.obchodnik.data.local.dao.PortfolioSnapshotDao
import cz.obchodnik.data.local.dao.QuoteDao
import cz.obchodnik.data.local.entity.AlertEntity
import cz.obchodnik.data.local.entity.AssetEntity
import cz.obchodnik.data.local.entity.HistoryEntity
import cz.obchodnik.data.local.entity.HoldingEntity
import cz.obchodnik.data.local.entity.PortfolioSnapshotEntity
import cz.obchodnik.data.local.entity.QuoteEntity

@Database(
    entities = [
        AssetEntity::class,
        QuoteEntity::class,
        HistoryEntity::class,
        HoldingEntity::class,
        AlertEntity::class,
        PortfolioSnapshotEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class ObchodnikDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun quoteDao(): QuoteDao
    abstract fun historyDao(): HistoryDao
    abstract fun holdingDao(): HoldingDao
    abstract fun alertDao(): AlertDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alerts ADD COLUMN repeating INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alerts ADD COLUMN armed INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `portfolio_snapshots` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`dayStartMillis` INTEGER NOT NULL, " +
                        "`totalValue` REAL NOT NULL, " +
                        "`totalInvested` REAL NOT NULL, " +
                        "`currency` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_portfolio_snapshots_dayStartMillis_currency` " +
                        "ON `portfolio_snapshots` (`dayStartMillis`, `currency`)",
                )
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
    }
}
