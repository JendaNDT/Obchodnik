package cz.obchodnik.data.repository

import cz.obchodnik.data.local.dao.HoldingDao
import cz.obchodnik.data.local.toDomain
import cz.obchodnik.data.local.toEntity
import cz.obchodnik.domain.model.Holding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PortfolioRepository(
    private val holdingDao: HoldingDao,
) {
    fun observeHoldings(): Flow<List<Holding>> =
        holdingDao.observeHoldings().map { holdings -> holdings.map { it.toDomain() } }

    suspend fun save(holding: Holding) {
        holdingDao.upsertHolding(holding.toEntity())
    }

    suspend fun delete(holding: Holding) {
        holdingDao.deleteHolding(holding.toEntity())
    }
}
