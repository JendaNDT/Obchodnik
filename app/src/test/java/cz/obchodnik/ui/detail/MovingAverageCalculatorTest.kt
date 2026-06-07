package cz.obchodnik.ui.detail

import cz.obchodnik.domain.model.PricePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovingAverageCalculatorTest {

    @Test
    fun `simple moving average returns empty list when window is larger than points`() {
        val result = MovingAverageCalculator.simpleMovingAverage(
            points = listOf(point(1, 10.0), point(2, 20.0)),
            window = 3,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `simple moving average keeps timestamp of closing point`() {
        val result = MovingAverageCalculator.simpleMovingAverage(
            points = listOf(
                point(1, 10.0),
                point(2, 20.0),
                point(3, 30.0),
                point(4, 60.0),
            ),
            window = 3,
        )

        assertEquals(2, result.size)
        assertEquals(3L, result[0].timestamp)
        assertEquals(20.0, result[0].price, 0.001)
        assertEquals(4L, result[1].timestamp)
        assertEquals(36.666, result[1].price, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `simple moving average rejects non positive window`() {
        MovingAverageCalculator.simpleMovingAverage(points = listOf(point(1, 10.0)), window = 0)
    }

    private fun point(timestamp: Long, price: Double): PricePoint =
        PricePoint(timestamp = timestamp, price = price)
}
