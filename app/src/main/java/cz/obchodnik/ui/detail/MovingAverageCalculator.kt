package cz.obchodnik.ui.detail

import cz.obchodnik.domain.model.PricePoint

object MovingAverageCalculator {
    fun simpleMovingAverage(points: List<PricePoint>, window: Int): List<PricePoint> {
        require(window > 0) { "Window must be positive" }
        if (points.size < window) return emptyList()
        val result = ArrayList<PricePoint>(points.size - window + 1)
        var sum = 0.0
        points.forEachIndexed { index, point ->
            sum += point.price
            if (index >= window) {
                sum -= points[index - window].price
            }
            if (index >= window - 1) {
                result.add(point.copy(price = sum / window))
            }
        }
        return result
    }
}
