package cz.obchodnik.data.repository

import cz.obchodnik.data.local.dao.HoldingDao
import cz.obchodnik.data.local.dao.PortfolioSnapshotDao
import cz.obchodnik.data.local.toDomain
import cz.obchodnik.data.local.toEntity
import cz.obchodnik.domain.model.Holding
import cz.obchodnik.domain.model.PortfolioSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PortfolioRepository(
    private val holdingDao: HoldingDao,
    private val snapshotDao: PortfolioSnapshotDao,
) {
    fun observeHoldings(): Flow<List<Holding>> =
        holdingDao.observeHoldings().map { holdings -> holdings.map { it.toDomain() } }

    suspend fun save(holding: Holding) {
        holdingDao.upsertHolding(holding.toEntity())
    }

    suspend fun delete(holding: Holding) {
        holdingDao.deleteHolding(holding.toEntity())
    }

    fun observeSnapshots(currency: String): Flow<List<PortfolioSnapshot>> =
        snapshotDao.observeSnapshots(currency.lowercase())
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun recordSnapshot(
        dayStartMillis: Long,
        totalValue: Double,
        totalInvested: Double,
        currency: String,
    ) {
        snapshotDao.upsert(
            PortfolioSnapshot(
                id = 0,
                dayStartMillis = dayStartMillis,
                totalValue = totalValue,
                totalInvested = totalInvested,
                currency = currency.lowercase(),
            ).toEntity(),
        )
    }

    suspend fun getSnapshotForDay(dayStartMillis: Long, currency: String): PortfolioSnapshot? {
        return snapshotDao.getSnapshotForDay(dayStartMillis, currency.lowercase())?.toDomain()
    }
}
