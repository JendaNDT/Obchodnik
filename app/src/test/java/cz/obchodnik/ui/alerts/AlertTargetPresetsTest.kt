package cz.obchodnik.ui.alerts

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertTargetPresetsTest {

    @Test
    fun `target from percent increases reference price`() {
        val target = AlertTargetPresets.targetFromPercent(referencePrice = 100.0, percentChange = 5.0)

        assertEquals(105.0, target, 0.001)
    }

    @Test
    fun `target from percent decreases reference price`() {
        val target = AlertTargetPresets.targetFromPercent(referencePrice = 100.0, percentChange = -10.0)

        assertEquals(90.0, target, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `target from percent rejects non positive reference price`() {
        AlertTargetPresets.targetFromPercent(referencePrice = 0.0, percentChange = 5.0)
    }
}
