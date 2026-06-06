package cz.obchodnik.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cz.obchodnik.data.local.QuoteLocalStore
import cz.obchodnik.data.local.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao : QuoteLocalStore {
    @Upsert
    override suspend fun upsertQuotes(quotes: List<QuoteEntity>)

    @Query("SELECT * FROM quotes WHERE currency = :currency ORDER BY assetId ASC")
    override fun observeQuotes(currency: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE currency = :currency AND assetId IN (:assetIds)")
    override suspend fun quotesForAssets(assetIds: List<String>, currency: String): List<QuoteEntity>
}
