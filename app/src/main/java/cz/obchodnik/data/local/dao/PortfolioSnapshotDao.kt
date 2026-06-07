package cz.obchodnik.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cz.obchodnik.data.local.entity.PortfolioSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioSnapshotDao {
    @Query("SELECT * FROM portfolio_snapshots WHERE currency = :currency ORDER BY dayStartMillis ASC")
    fun observeSnapshots(currency: String): Flow<List<PortfolioSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: PortfolioSnapshotEntity)
}
