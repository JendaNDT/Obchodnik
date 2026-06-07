package cz.obchodnik.ui.alerts

object AlertTargetPresets {
    val percentChanges: List<Double> = listOf(5.0, 10.0, -5.0, -10.0)

    fun targetFromPercent(referencePrice: Double, percentChange: Double): Double {
        require(referencePrice > 0.0) { "Reference price must be positive" }
        return referencePrice * (1.0 + percentChange / 100.0)
    }
}
