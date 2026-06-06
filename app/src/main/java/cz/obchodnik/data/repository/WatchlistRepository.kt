package cz.obchodnik.data.repository

import cz.obchodnik.data.local.AssetLocalStore
import cz.obchodnik.data.local.toDomain
import cz.obchodnik.data.local.toEntity
import cz.obchodnik.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WatchlistRepository(
    private val assetStore: AssetLocalStore,
) {
    fun observeWatchlist(): Flow<List<Asset>> =
        assetStore.observeWatchlistAssets().map { assets ->
            assets.map { it.toDomain() }
        }

    suspend fun watchlist(): List<Asset> =
        assetStore.watchlistAssets().map { it.toDomain() }

    suspend fun assetById(assetId: String): Asset? =
        assetStore.assetById(assetId)?.toDomain()

    suspend fun saveCandidate(asset: Asset) {
        val existing = assetStore.assetById(asset.id)
        assetStore.upsertAsset(
            asset.toEntity(
                inWatchlist = existing?.inWatchlist ?: false,
                sortOrder = existing?.sortOrder ?: Int.MAX_VALUE,
            ),
        )
    }

    suspend fun add(asset: Asset) {
        val existing = assetStore.assetById(asset.id)
        val sortOrder = existing?.sortOrder?.takeIf { existing.inWatchlist }
            ?: ((assetStore.maxSortOrder() ?: -1) + 1)
        assetStore.upsertAsset(asset.toEntity(inWatchlist = true, sortOrder = sortOrder))
    }

    suspend fun remove(assetId: String) {
        assetStore.setWatchlistState(assetId, inWatchlist = false, sortOrder = Int.MAX_VALUE)
    }
}
