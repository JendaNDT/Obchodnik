package cz.obchodnik.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import cz.obchodnik.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY enabled DESC, id DESC")
    fun observeAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE enabled = 1")
    suspend fun enabledAlerts(): List<AlertEntity>

    @Upsert
    suspend fun upsertAlert(alert: AlertEntity)

    @Delete
    suspend fun deleteAlert(alert: AlertEntity)
}
