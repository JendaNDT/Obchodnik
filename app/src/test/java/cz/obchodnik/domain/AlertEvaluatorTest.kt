package cz.obchodnik.domain

import cz.obchodnik.domain.model.PriceAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEvaluatorTest {

    private fun alert(
        above: Boolean = true,
        target: Double = 100.0,
        enabled: Boolean = true,
        repeating: Boolean = false,
        armed: Boolean = true,
    ) = PriceAlert(
        id = 1,
        assetId = "cg:bitcoin",
        above = above,
        target = target,
        enabled = enabled,
        triggeredAt = null,
        repeating = repeating,
        armed = armed,
    )

    @Test
    fun `disabled alert is ignored`() {
        assertNull(AlertEvaluator.evaluate(alert(enabled = false), 150.0, "usd", 1L))
    }

    @Test
    fun `one shot above fires and disables`() {
        val outcome = AlertEvaluator.evaluate(alert(), 120.0, "usd", 42L)!!
        assertTrue(outcome.notify)
        assertFalse(outcome.updatedAlert.enabled)
        assertEquals(42L, outcome.updatedAlert.triggeredAt)
        assertEquals(120.0, outcome.updatedAlert.triggeredPrice!!, 0.0)
        assertEquals("usd", outcome.updatedAlert.triggeredCurrency)
    }

    @Test
    fun `one shot does not fire below target`() {
        assertNull(AlertEvaluator.evaluate(alert(), 90.0, "usd", 1L))
    }

    @Test
    fun `below alert fires when price drops to target`() {
        val outcome = AlertEvaluator.evaluate(alert(above = false, target = 50.0), 50.0, "usd", 1L)!!
        assertTrue(outcome.notify)
        assertFalse(outcome.updatedAlert.enabled)
    }

    @Test
    fun `repeating fires but stays enabled and disarms`() {
        val outcome = AlertEvaluator.evaluate(alert(repeating = true), 120.0, "usd", 7L)!!
        assertTrue(outcome.notify)
        assertTrue(outcome.updatedAlert.enabled)
        assertFalse(outcome.updatedAlert.armed)
        assertEquals(7L, outcome.updatedAlert.triggeredAt)
    }

    @Test
    fun `repeating does not refire while disarmed and condition holds`() {
        assertNull(AlertEvaluator.evaluate(alert(repeating = true, armed = false), 130.0, "usd", 9L))
    }

    @Test
    fun `repeating re-arms silently when price returns past target`() {
        val outcome = AlertEvaluator.evaluate(alert(repeating = true, armed = false), 80.0, "usd", 9L)!!
        assertFalse(outcome.notify)
        assertTrue(outcome.updatedAlert.armed)
        assertTrue(outcome.updatedAlert.enabled)
    }

    @Test
    fun `repeating fires again after being re-armed`() {
        val rearmed = AlertEvaluator.evaluate(alert(repeating = true, armed = false), 80.0, "usd", 1L)!!.updatedAlert
        val refire = AlertEvaluator.evaluate(rearmed, 121.0, "usd", 2L)!!
        assertTrue(refire.notify)
        assertFalse(refire.updatedAlert.armed)
    }
}
