package cz.obchodnik.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.core.Result
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.repository.MarketRepository
import cz.obchodnik.data.repository.WatchlistRepository
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.StaticAssetCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(
    private val assetId: String,
    private val watchlistRepository: WatchlistRepository,
    private val marketRepository: MarketRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadAssetAndData(force = false)
        }
    }

    fun setChartMode(mode: ChartMode) {
        _uiState.update { it.copy(chartMode = mode) }
        refreshChart(force = false)
    }

    fun setRange(range: ChartRange) {
        _uiState.update { it.copy(range = range) }
        refreshChart(force = false)
    }

    fun toggleWatch() {
        viewModelScope.launch {
            val asset = _uiState.value.asset ?: return@launch
            if (_uiState.value.inWatchlist) {
                watchlistRepository.remove(asset.id)
            } else {
                watchlistRepository.add(asset)
            }
            val watched = watchlistRepository.watchlist().any { it.id == asset.id }
            _uiState.update { it.copy(inWatchlist = watched) }
        }
    }

    private suspend fun loadAssetAndData(force: Boolean) {
        val currency = settingsRepository.settings.first().currency
        val asset = resolveAsset()
        if (asset == null) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Aktivum se nepodařilo najít.")
            }
            return
        }

        val watched = watchlistRepository.watchlist().any { it.id == asset.id }
        _uiState.update {
            it.copy(
                asset = asset,
                currency = currency,
                inWatchlist = watched,
                isLoading = true,
                errorMessage = null,
                noticeMessage = null,
            )
        }
        val quoteResult = marketRepository.refreshQuotes(listOf(asset), currency, force = force)
        val quote = marketRepository.cachedQuote(asset.id, currency)
        _uiState.update {
            it.copy(
                quote = quote,
                noticeMessage = (quoteResult as? Result.Success)?.notice,
            )
        }
        refreshChart(force)
    }

    private fun refreshChart(force: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val asset = state.asset ?: return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (state.chartMode == ChartMode.LINE) {
                marketRepository.history(asset, state.range, state.currency, force)
            } else {
                marketRepository.candles(asset, state.range, state.currency, force)
            }
            when (result) {
                is Result.Success<*> -> {
                    if (state.chartMode == ChartMode.LINE) {
                        @Suppress("UNCHECKED_CAST")
                        _uiState.update {
                            it.copy(
                                linePoints = result.data as List<cz.obchodnik.domain.model.PricePoint>,
                                isLoading = false,
                                noticeMessage = result.notice ?: it.noticeMessage,
                            )
                        }
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        _uiState.update {
                            it.copy(
                                candles = result.data as List<cz.obchodnik.domain.model.Candle>,
                                isLoading = false,
                                noticeMessage = result.notice ?: it.noticeMessage,
                            )
                        }
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    private suspend fun resolveAsset(): Asset? =
        watchlistRepository.assetById(assetId)
            ?: StaticAssetCatalog.assets.firstOrNull { it.id == assetId }

    companion object {
        fun factory(app: ObchodnikApp, assetId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DetailViewModel(
                        assetId = assetId,
                        watchlistRepository = app.container.watchlistRepository,
                        marketRepository = app.container.marketRepository,
                        settingsRepository = app.container.settingsRepository,
                    ) as T
                }
            }
    }
}
