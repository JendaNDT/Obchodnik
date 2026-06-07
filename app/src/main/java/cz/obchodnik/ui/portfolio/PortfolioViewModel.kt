package cz.obchodnik.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.repository.MarketRepository
import cz.obchodnik.data.repository.PortfolioRepository
import cz.obchodnik.data.repository.WatchlistRepository
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Holding
import cz.obchodnik.domain.model.Quote
import cz.obchodnik.domain.model.StaticAssetCatalog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PortfolioUiState(
    val items: List<PortfolioItem> = emptyList(),
    val totalValue: Double = 0.0,
    val totalInvested: Double = 0.0,
    val totalPL: Double = 0.0,
    val totalPLPct: Double? = null,
    val currency: String = "usd",
    val allWatchlistAssets: List<Asset> = emptyList(),
    val isLoading: Boolean = false,
)

data class PortfolioItem(
    val holding: Holding,
    val asset: Asset,
    val quote: Quote?,
    val value: Double,
    val invested: Double,
    val plValue: Double,
    val plPct: Double?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModel(
    private val portfolioRepository: PortfolioRepository,
    private val watchlistRepository: WatchlistRepository,
    private val marketRepository: MarketRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<PortfolioUiState> = settingsRepository.settings
        .map { it.currency }
        .flatMapLatest { currency ->
            combine(
                portfolioRepository.observeHoldings(),
                marketRepository.observeQuotes(currency),
                watchlistRepository.observeWatchlist()
            ) { holdings, quotes, watchlist ->
                val items = holdings.mapNotNull { holding ->
                    val asset = watchlist.firstOrNull { it.id == holding.assetId }
                        ?: StaticAssetCatalog.assets.firstOrNull { it.id == holding.assetId }
                        ?: return@mapNotNull null
                    val quote = quotes.firstOrNull { it.assetId == holding.assetId }

                    val currentPrice = quote?.price ?: holding.avgPrice
                    val value = holding.qty * currentPrice
                    val invested = holding.qty * holding.avgPrice
                    val plValue = value - invested
                    val plPct = if (invested > 0.0) (plValue / invested) * 100.0 else null

                    PortfolioItem(
                        holding = holding,
                        asset = asset,
                        quote = quote,
                        value = value,
                        invested = invested,
                        plValue = plValue,
                        plPct = plPct
                    )
                }

                val totalValue = items.sumOf { it.value }
                val totalInvested = items.sumOf { it.invested }
                val totalPL = totalValue - totalInvested
                val totalPLPct = if (totalInvested > 0.0) (totalPL / totalInvested) * 100.0 else null

                PortfolioUiState(
                    items = items,
                    totalValue = totalValue,
                    totalInvested = totalInvested,
                    totalPL = totalPL,
                    totalPLPct = totalPLPct,
                    currency = currency,
                    allWatchlistAssets = watchlist,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PortfolioUiState(isLoading = true)
        )

    fun addPosition(assetId: String, qty: Double, avgPrice: Double) {
        viewModelScope.launch {
            val holding = Holding(
                id = 0,
                assetId = assetId,
                qty = qty,
                avgPrice = avgPrice
            )
            portfolioRepository.save(holding)
        }
    }

    fun updatePosition(holding: Holding, qty: Double, avgPrice: Double) {
        viewModelScope.launch {
            portfolioRepository.save(
                holding.copy(
                    qty = qty,
                    avgPrice = avgPrice,
                ),
            )
        }
    }

    fun deletePosition(holding: Holding) {
        viewModelScope.launch {
            portfolioRepository.delete(holding)
        }
    }

    companion object {
        fun factory(app: ObchodnikApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PortfolioViewModel(
                        portfolioRepository = app.container.portfolioRepository,
                        watchlistRepository = app.container.watchlistRepository,
                        marketRepository = app.container.marketRepository,
                        settingsRepository = app.container.settingsRepository,
                    ) as T
                }
            }
    }
}
