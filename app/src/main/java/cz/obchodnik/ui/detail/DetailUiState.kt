package cz.obchodnik.ui.detail

import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote

data class DetailUiState(
    val asset: Asset? = null,
    val quote: Quote? = null,
    val chartMode: ChartMode = ChartMode.LINE,
    val range: ChartRange = ChartRange.W1,
    val linePoints: List<PricePoint> = emptyList(),
    val sma7Points: List<PricePoint> = emptyList(),
    val sma30Points: List<PricePoint> = emptyList(),
    val showSma7: Boolean = false,
    val showSma30: Boolean = false,
    val candles: List<Candle> = emptyList(),
    val currency: String = "usd",
    val inWatchlist: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
)

enum class ChartMode(val label: String) {
    LINE("Křivka"),
    CANDLE("Svíčky"),
}
