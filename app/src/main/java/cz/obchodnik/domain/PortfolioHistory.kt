package cz.obchodnik.domain

import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.PortfolioSnapshot
import cz.obchodnik.domain.model.PricePoint
import java.util.concurrent.TimeUnit

/** Pure helpers for portfolio value-over-time snapshots and charting. */
object PortfolioHistory {

    private val dayMillis = TimeUnit.DAYS.toMillis(1)

    /** Truncates an epoch-millis instant to the start of its UTC day. */
    fun startOfDayMillis(epochMillis: Long): Long = (epochMillis / dayMillis) * dayMillis

    /**
     * Filters [snapshots] (assumed already in the target currency, ascending by day)
     * to [range] relative to [now] and maps them to chart points.
     */
    fun pointsForRange(
        snapshots: List<PortfolioSnapshot>,
        range: ChartRange,
        now: Long,
    ): List<PricePoint> {
        val cutoff = when (range) {
            ChartRange.D1 -> now - dayMillis
            ChartRange.W1 -> now - 7 * dayMillis
            ChartRange.M1 -> now - 30 * dayMillis
            ChartRange.Y1 -> now - 365 * dayMillis
            ChartRange.ALL -> Long.MIN_VALUE
        }
        return snapshots
            .filter { it.dayStartMillis >= cutoff }
            .sortedBy { it.dayStartMillis }
            .map { PricePoint(timestamp = it.dayStartMillis, price = it.totalValue) }
    }
}
