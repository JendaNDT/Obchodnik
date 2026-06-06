package cz.obchodnik.data.repository

import cz.obchodnik.data.local.dao.AlertDao
import cz.obchodnik.data.local.toDomain
import cz.obchodnik.data.local.toEntity
import cz.obchodnik.domain.model.PriceAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlertRepository(
    private val alertDao: AlertDao,
) {
    fun observeAlerts(): Flow<List<PriceAlert>> =
        alertDao.observeAlerts().map { alerts -> alerts.map { it.toDomain() } }

    suspend fun enabledAlerts(): List<PriceAlert> =
        alertDao.enabledAlerts().map { it.toDomain() }

    suspend fun save(alert: PriceAlert) {
        alertDao.upsertAlert(alert.toEntity())
    }

    suspend fun delete(alert: PriceAlert) {
        alertDao.deleteAlert(alert.toEntity())
    }
}
