package cz.obchodnik.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cz.obchodnik.data.local.AssetLocalStore
import cz.obchodnik.data.local.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao : AssetLocalStore {
    @Upsert
    override suspend fun upsertAssets(assets: List<AssetEntity>)

    @Upsert
    override suspend fun upsertAsset(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    override suspend fun assetById(id: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE inWatchlist = 1 ORDER BY sortOrder ASC, symbol ASC")
    override fun observeWatchlistAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE inWatchlist = 1 ORDER BY sortOrder ASC, symbol ASC")
    override suspend fun watchlistAssets(): List<AssetEntity>

    @Query("SELECT MAX(sortOrder) FROM assets WHERE inWatchlist = 1")
    override suspend fun maxSortOrder(): Int?

    @Query("UPDATE assets SET inWatchlist = :inWatchlist, sortOrder = :sortOrder WHERE id = :id")
    override suspend fun setWatchlistState(id: String, inWatchlist: Boolean, sortOrder: Int)
}
