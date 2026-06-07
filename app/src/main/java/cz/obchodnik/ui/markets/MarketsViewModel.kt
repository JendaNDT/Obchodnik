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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MarketsViewModel(
    private val watchlistRepository: WatchlistRepository,
    private val marketRepository: MarketRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow(MarketCategory.ALL)
    private val query = MutableStateFlow("")
    private val sortMode = MutableStateFlow(MarketSortMode.MANUAL)
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val noticeMessage = MutableStateFlow<String?>(null)
    private val refreshMessages = combine(errorMessage, noticeMessage) { error, notice -> error to notice }
    private val controls = combine(selectedCategory, query, sortMode) { category, searchQuery, sort ->
        MarketControls(category = category, query = searchQuery, sortMode = sort)
    }

    val uiState = combine(
        watchlistRepository.observeWatchlist(),
        settingsRepository.settings.flatMapLatest { settings ->
            marketRepository.observeQuotes(settings.currency).map { quotes -> settings to quotes }
        },
        controls,
        isRefreshing,
        refreshMessages,
    ) { watchlist, settingsAndQuotes, controls, refreshing, messages ->
        val (settings, quotes) = settingsAndQuotes
        val (error, notice) = messages
        val filtered = MarketListTransformer.transform(
            watchlist = watchlist,
            quotes = quotes,
            category = controls.category,
            query = controls.query,
            sortMode = controls.sortMode,
        )
        MarketsUiState(
            assets = filtered,
            selectedCategory = controls.category,
            query = controls.query,
            sortMode = controls.sortMode,
            activeQuickView = MarketQuickView.entries.firstOrNull {
                it.category == controls.category &&
                    it.sortMode == controls.sortMode &&
                    it.query == controls.query
            },
            savedViews = SavedMarketViewSerializer.decode(settings.savedMarketViewsJson),
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

    fun updateQuery(value: String) {
        query.value = value
    }

    fun selectSortMode(mode: MarketSortMode) {
        sortMode.value = mode
    }

    fun applyQuickView(view: MarketQuickView) {
        selectedCategory.value = view.category
        query.value = view.query
        sortMode.value = view.sortMode
    }

    fun applySavedView(view: SavedMarketView) {
        selectedCategory.value = view.category
        query.value = view.query
        sortMode.value = view.sortMode
    }

    fun saveCurrentView(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            val currentJson = settingsRepository.settings.first().savedMarketViewsJson
            val views = SavedMarketViewSerializer.decode(currentJson)
            val newView = SavedMarketView(
                id = UUID.randomUUID().toString(),
                name = trimmedName,
                category = selectedCategory.value,
                sortMode = sortMode.value,
                query = query.value.trim(),
            )
            settingsRepository.setSavedMarketViewsJson(
                SavedMarketViewSerializer.encode(views + newView),
            )
        }
    }

    fun deleteSavedView(id: String) {
        viewModelScope.launch {
            val currentJson = settingsRepository.settings.first().savedMarketViewsJson
            val views = SavedMarketViewSerializer.decode(currentJson)
            settingsRepository.setSavedMarketViewsJson(
                SavedMarketViewSerializer.encode(views.filterNot { it.id == id }),
            )
        }
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

    fun moveAsset(assetId: String, offset: Int) {
        viewModelScope.launch {
            watchlistRepository.move(assetId, offset)
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

private data class MarketControls(
    val category: MarketCategory,
    val query: String,
    val sortMode: MarketSortMode,
)
