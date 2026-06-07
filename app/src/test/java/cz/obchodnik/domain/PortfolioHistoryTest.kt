package cz.obchodnik.domain

import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.PortfolioSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioHistoryTest {

    private val day = 24L * 60 * 60 * 1000

    private fun snap(dayStart: Long, value: Double, currency: String = "usd") =
        PortfolioSnapshot(
            id = 0,
            dayStartMillis = dayStart,
            totalValue = value,
            totalInvested = 0.0,
            currency = currency,
        )

    @Test
    fun `startOfDayMillis truncates to UTC day`() {
        val noon = 10 * day + 12L * 60 * 60 * 1000
        assertEquals(10 * day, PortfolioHistory.startOfDayMillis(noon))
        assertEquals(10 * day, PortfolioHistory.startOfDayMillis(10 * day))
    }

    @Test
    fun `pointsForRange keeps only snapshots within the window`() {
        val now = 100 * day
        val snapshots = listOf(
            snap(60 * day, 1.0),
            snap(80 * day, 2.0),
            snap(99 * day, 3.0),
        )
        // 1M window = now - 30d = 70d cutoff -> keeps 80d and 99d
        val points = PortfolioHistory.pointsForRange(snapshots, ChartRange.M1, now)
        assertEquals(2, points.size)
        assertEquals(80 * day, points.first().timestamp)
        assertEquals(3.0, points.last().price, 0.0)
    }

    @Test
    fun `pointsForRange ALL keeps everything sorted ascending`() {
        val now = 100 * day
        val snapshots = listOf(
            snap(99 * day, 3.0),
            snap(10 * day, 1.0),
            snap(50 * day, 2.0),
        )
        val points = PortfolioHistory.pointsForRange(snapshots, ChartRange.ALL, now)
        assertEquals(3, points.size)
        assertEquals(10 * day, points.first().timestamp)
        assertEquals(99 * day, points.last().timestamp)
    }

    @Test
    fun `pointsForRange maps value to price`() {
        val now = 5 * day
        val points = PortfolioHistory.pointsForRange(listOf(snap(4 * day, 12345.0)), ChartRange.W1, now)
        assertEquals(1, points.size)
        assertEquals(12345.0, points.first().price, 0.0)
        assertTrue(points.first().timestamp == 4 * day)
    }
}
