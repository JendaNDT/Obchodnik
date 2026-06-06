package cz.obchodnik.data.local

import cz.obchodnik.data.local.entity.AssetEntity
import cz.obchodnik.data.local.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

interface AssetLocalStore {
    suspend fun upsertAssets(assets: List<AssetEntity>)
    suspend fun upsertAsset(asset: AssetEntity)
    suspend fun assetById(id: String): AssetEntity?
    fun observeWatchlistAssets(): Flow<List<AssetEntity>>
    suspend fun watchlistAssets(): List<AssetEntity>
    suspend fun maxSortOrder(): Int?
    suspend fun setWatchlistState(id: String, inWatchlist: Boolean, sortOrder: Int)
}

interface QuoteLocalStore {
    suspend fun upsertQuotes(quotes: List<QuoteEntity>)
    fun observeQuotes(currency: String): Flow<List<QuoteEntity>>
    suspend fun quotesForAssets(assetIds: List<String>, currency: String): List<QuoteEntity>
}
