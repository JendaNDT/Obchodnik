package cz.obchodnik.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.repository.MarketRepository
import cz.obchodnik.data.repository.WatchlistRepository
import cz.obchodnik.domain.model.StaticAssetCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 1,
    val selectedAssetIds: Set<String> = setOf("cg:bitcoin", "cg:ethereum", "cg:solana", "cg:pax-gold"),
    val notificationsPermissionRequested: Boolean = false,
)

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
    private val watchlistRepository: WatchlistRepository,
    private val marketRepository: MarketRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(4)) }
    }

    fun previousStep() {
        _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(1)) }
    }

    fun toggleAsset(assetId: String) {
        _uiState.update { state ->
            val current = state.selectedAssetIds
            val updated = if (current.contains(assetId)) {
                current - assetId
            } else {
                current + assetId
            }
            state.copy(selectedAssetIds = updated)
        }
    }

    fun completeOnboarding(currency: String) {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedAssetIds
            val chosenAssets = StaticAssetCatalog.assets.filter { it.id in selectedIds }

            // 1. Add chosen assets to watchlist
            chosenAssets.forEach { asset ->
                watchlistRepository.add(asset)
            }

            // 2. Prefetch prices for chosen assets
            marketRepository.refreshQuotes(chosenAssets, currency, force = true)

            // 3. Mark onboarding as done
            settingsRepository.setOnboardingDone(true)
        }
    }

    companion object {
        fun factory(app: ObchodnikApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(
                        settingsRepository = app.container.settingsRepository,
                        watchlistRepository = app.container.watchlistRepository,
                        marketRepository = app.container.marketRepository,
                    ) as T
                }
            }
    }
}
