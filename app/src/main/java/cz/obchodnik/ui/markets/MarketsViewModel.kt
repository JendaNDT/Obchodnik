package cz.obchodnik.ui.markets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.core.Result
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.repository.MarketRepository
import cz.obchodnik.data.repository.WatchlistRepository
import cz.obchodnik.domain.model.DefaultAssets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class MarketsViewModel(
    private val watchlistRepository: WatchlistRepository,
    private val marketRepository: MarketRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow(MarketCategory.ALL)
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val noticeMessage = MutableStateFlow<String?>(null)
    private val refreshMessages = combine(errorMessage, noticeMessage) { error, notice -> error to notice }

    val uiState = combine(
        watchlistRepository.observeWatchlist(),
        settingsRepository.settings.flatMapLatest { settings ->
            marketRepository.observeQuotes(settings.currency).map { quotes -> settings to quotes }
        },
        selectedCategory,
        isRefreshing,
        refreshMessages,
    ) { watchlist, settingsAndQuotes, category, refreshing, messages ->
        val (settings, quotes) = settingsAndQuotes
        val (error, notice) = messages
        val quotesByAsset = quotes.associateBy { it.assetId }
        val filtered = watchlist
            .filter { category.includes(it.type) }
            .map { asset -> MarketAssetUi(asset = asset, quote = quotesByAsset[asset.id]) }
        MarketsUiState(
            assets = filtered,
            selectedCategory = category,
            currency = settings.currency,
            isLoading = watchlist.isEmpty() && refreshing,
            isRefreshing = refreshing,
            errorMessage = error,
            noticeMessage = notice,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MarketsUiState(),
    )

    init {
        viewModelScope.launch {
            seedDefaultWatchlistIfNeeded()
            refresh(force = false)
        }
    }

    fun selectCategory(category: MarketCategory) {
        selectedCategory.value = category
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            isRefreshing.value = true
            val watchlist = watchlistRepository.watchlist()
            val currency = settingsRepository.settings.first().currency
            when (val result = marketRepository.refreshQuotes(watchlist, currency, force)) {
                is Result.Error -> {
                    errorMessage.value = result.message
                    noticeMessage.value = null
                }
                is Result.Success -> {
                    errorMessage.value = null
                    noticeMessage.value = result.notice
                }
                Result.Loading -> Unit
            }
            isRefreshing.value = false
        }
    }

    private suspend fun seedDefaultWatchlistIfNeeded() {
        if (watchlistRepository.watchlist().isNotEmpty()) return
        DefaultAssets.watchlist.forEach { asset ->
            watchlistRepository.add(asset)
        }
    }

    companion object {
        fun factory(app: ObchodnikApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MarketsViewModel(
                        watchlistRepository = app.container.watchlistRepository,
                        marketRepository = app.container.marketRepository,
                        settingsRepository = app.container.settingsRepository,
                    ) as T
                }
            }
    }
}
