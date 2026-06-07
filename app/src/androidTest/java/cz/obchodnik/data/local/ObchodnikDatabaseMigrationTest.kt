package cz.obchodnik.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ObchodnikDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ObchodnikDatabase::class.java,
    )

    @Test
    fun migratesFromVersion2ToCurrentSchema() {
        helper.createDatabase(TEST_DB, 2).apply {
            insertVersion2SeedRows()
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            *ObchodnikDatabase.MIGRATIONS,
        )

        assertEquals(1, db.queryLong("SELECT COUNT(*) FROM assets"))
        assertEquals(1, db.queryLong("SELECT COUNT(*) FROM quotes"))
        assertEquals(1, db.queryLong("SELECT COUNT(*) FROM history"))
        assertEquals(1, db.queryLong("SELECT COUNT(*) FROM holdings"))
        assertEquals(1, db.queryLong("SELECT COUNT(*) FROM alerts"))
        db.query("SELECT triggeredPrice, triggeredCurrency FROM alerts WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(true, cursor.isNull(0))
            assertEquals(true, cursor.isNull(1))
        }
        db.query("SELECT repeating, armed FROM alerts WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0L, cursor.getLong(0))
            assertEquals(1L, cursor.getLong(1))
        }
        assertEquals(0, db.queryLong("SELECT COUNT(*) FROM portfolio_snapshots"))
    }

    private fun SupportSQLiteDatabase.insertVersion2SeedRows() {
        execSQL(
            """
            INSERT INTO assets (
                id, symbol, name, type, provider, sourceId, colorHex, logoUrl, inWatchlist, sortOrder
            ) VALUES (
                'cg:bitcoin', 'BTC', 'Bitcoin', 'CRYPTO', 'COINGECKO', 'bitcoin', '#f7931a', NULL, 1, 0
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO quotes (
                assetId, currency, price, change24h, change7d, change30d, high24h, low24h,
                marketCap, volume24h, sparklineJson, updatedAt
            ) VALUES (
                'cg:bitcoin', 'usd', 50000.0, 1.5, 2.5, 3.5, 51000.0, 49000.0,
                1000000.0, 500000.0, '[1.0,2.0]', 1700000000000
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO history (
                assetId, range, currency, kind, pointsJson, updatedAt
            ) VALUES (
                'cg:bitcoin', 'D1', 'usd', 'line', '[{"time":1700000000000,"price":50000.0}]', 1700000000000
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO holdings (
                id, assetId, qty, avgPrice
            ) VALUES (
                1, 'cg:bitcoin', 0.25, 45000.0
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO alerts (
                id, assetId, above, target, enabled, triggeredAt
            ) VALUES (
                1, 'cg:bitcoin', 1, 70000.0, 1, NULL
            )
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.queryLong(sql: String): Long =
        query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }

    private companion object {
        const val TEST_DB = "obchodnik-migration-test"
    }
}
