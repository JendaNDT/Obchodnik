package cz.obchodnik.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.core.Result
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.repository.MarketRepository
import cz.obchodnik.data.repository.WatchlistRepository
import cz.obchodnik.data.source.MarketDataSource
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.StaticAssetCatalog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SearchViewModel(
    private val watchlistRepository: WatchlistRepository,
    private val marketRepository: MarketRepository,
    private val marketDataSource: MarketDataSource,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SearchUiState(catalogResults = StaticAssetCatalog.assets),
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            watchlistRepository.observeWatchlist().collect { assets ->
                _uiState.update { it.copy(watchlistIds = assets.map { asset -> asset.id }.toSet()) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                catalogResults = StaticAssetCatalog.search(query),
                errorMessage = null,
            )
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            searchRemote(query)
        }
    }

    fun toggleWatch(asset: Asset) {
        viewModelScope.launch {
            val currentlyWatched = asset.id in _uiState.value.watchlistIds
            if (currentlyWatched) {
                watchlistRepository.remove(asset.id)
            } else {
                watchlistRepository.add(asset)
                if (asset.source == DataProvider.COINGECKO) {
                    val currency = settingsRepository.settings.first().currency
                    marketRepository.refreshQuotes(listOf(asset), currency, force = true)
                }
            }
        }
    }

    fun openAsset(asset: Asset, onReady: (String) -> Unit) {
        viewModelScope.launch {
            watchlistRepository.saveCandidate(asset)
            onReady(asset.id)
        }
    }

    private suspend fun searchRemote(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(remoteResults = emptyList(), isSearching = false) }
            return
        }
        _uiState.update { it.copy(isSearching = true) }
        when (val result = marketDataSource.search(query)) {
            is Result.Success -> _uiState.update {
                it.copy(remoteResults = result.data.distinctBy { asset -> asset.id }, isSearching = false)
            }
            is Result.Error -> _uiState.update {
                it.copy(remoteResults = emptyList(), isSearching = false, errorMessage = result.message)
            }
            Result.Loading -> Unit
        }
    }

    companion object {
        fun factory(app: ObchodnikApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(
                        watchlistRepository = app.container.watchlistRepository,
                        marketRepository = app.container.marketRepository,
                        marketDataSource = app.container.cryptoDataSource,
                        settingsRepository = app.container.settingsRepository,
                    ) as T
                }
            }
    }
}
