package cz.obchodnik.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cz.obchodnik.data.local.entity.HistoryEntity

@Dao
interface HistoryDao {
    @Upsert
    suspend fun upsertHistory(history: HistoryEntity)

    @Query("SELECT * FROM history WHERE assetId = :assetId AND range = :range AND currency = :currency AND kind = :kind LIMIT 1")
    suspend fun history(assetId: String, range: String, currency: String, kind: String): HistoryEntity?
}
