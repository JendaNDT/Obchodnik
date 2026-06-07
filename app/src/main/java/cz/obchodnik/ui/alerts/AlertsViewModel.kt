package cz.obchodnik.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.repository.AlertRepository
import cz.obchodnik.data.repository.WatchlistRepository
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.PriceAlert
import cz.obchodnik.domain.model.StaticAssetCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlertsUiState(
    val items: List<AlertItem> = emptyList(),
    val allWatchlistAssets: List<Asset> = emptyList(),
    val currency: String = "usd",
    val isLoading: Boolean = false,
)

data class AlertItem(
    val alert: PriceAlert,
    val asset: Asset,
)

class AlertsViewModel(
    private val alertRepository: AlertRepository,
    private val watchlistRepository: WatchlistRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<AlertsUiState> = combine(
        alertRepository.observeAlerts(),
        watchlistRepository.observeWatchlist(),
        settingsRepository.settings.map { it.currency }
    ) { alerts, watchlist, currency ->
        val items = alerts.mapNotNull { alert ->
            val asset = watchlist.firstOrNull { it.id == alert.assetId }
                ?: StaticAssetCatalog.assets.firstOrNull { it.id == alert.assetId }
                ?: return@mapNotNull null
            AlertItem(alert, asset)
        }
        AlertsUiState(
            items = items,
            allWatchlistAssets = watchlist,
            currency = currency,
            isLoading = false
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AlertsUiState(isLoading = true)
    )

    fun addAlert(assetId: String, above: Boolean, target: Double) {
        viewModelScope.launch {
            val alert = PriceAlert(
                id = 0,
                assetId = assetId,
                above = above,
                target = target,
                enabled = true,
                triggeredAt = null,
                triggeredPrice = null,
                triggeredCurrency = null,
            )
            alertRepository.save(alert)
        }
    }

    fun toggleAlertEnabled(alert: PriceAlert, enabled: Boolean) {
        viewModelScope.launch {
            val updated = if (enabled) {
                alert.copy(
                    enabled = true,
                    triggeredAt = null,
                    triggeredPrice = null,
                    triggeredCurrency = null,
                )
            } else {
                alert.copy(enabled = false)
            }
            alertRepository.save(updated)
        }
    }

    fun reactivateAlert(alert: PriceAlert) {
        toggleAlertEnabled(alert, enabled = true)
    }

    fun deleteAlert(alert: PriceAlert) {
        viewModelScope.launch {
            alertRepository.delete(alert)
        }
    }

    companion object {
        fun factory(app: ObchodnikApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AlertsViewModel(
                        alertRepository = app.container.alertRepository,
                        watchlistRepository = app.container.watchlistRepository,
                        settingsRepository = app.container.settingsRepository,
                    ) as T
                }
            }
    }
}
