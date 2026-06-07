package cz.obchodnik.ui.portfolio

data class PortfolioInsights(
    val positionCount: Int = 0,
    val largestPosition: PortfolioItem? = null,
    val largestPositionSharePct: Double? = null,
    val bestPerformer: PortfolioItem? = null,
    val worstPerformer: PortfolioItem? = null,
)

object PortfolioInsightsCalculator {
    fun calculate(items: List<PortfolioItem>, totalValue: Double): PortfolioInsights {
        if (items.isEmpty()) return PortfolioInsights()
        val largest = items.maxByOrNull { it.value }
        val best = items.maxByOrNull { it.plValue }
        val worst = items.minByOrNull { it.plValue }
        val largestShare = largest
            ?.takeIf { totalValue > 0.0 }
            ?.let { (it.value / totalValue) * 100.0 }

        return PortfolioInsights(
            positionCount = items.size,
            largestPosition = largest,
            largestPositionSharePct = largestShare,
            bestPerformer = best,
            worstPerformer = worst,
        )
    }
}
