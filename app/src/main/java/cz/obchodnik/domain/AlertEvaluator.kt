package cz.obchodnik.domain

import cz.obchodnik.domain.model.PriceAlert

/** Result of evaluating an alert against a fresh price. */
data class AlertOutcome(
    val updatedAlert: PriceAlert,
    val notify: Boolean,
)

/**
 * Pure decision logic for price alerts, including the hysteresis used by repeating
 * alerts: a repeating alert fires once when the target is crossed, then disarms and
 * only re-arms (silently) once the price returns to the other side of the target.
 * One-shot alerts keep the original behaviour: fire once, then disable.
 */
object AlertEvaluator {

    /**
     * @param price current price in [currency]
     * @return the change to persist plus whether to notify, or null when nothing changes.
     */
    fun evaluate(alert: PriceAlert, price: Double, currency: String, now: Long): AlertOutcome? {
        if (!alert.enabled) return null
        val conditionMet = if (alert.above) price >= alert.target else price <= alert.target

        return when {
            conditionMet && alert.armed -> {
                val fired = if (alert.repeating) {
                    alert.copy(
                        armed = false,
                        triggeredAt = now,
                        triggeredPrice = price,
                        triggeredCurrency = currency,
                    )
                } else {
                    alert.copy(
                        enabled = false,
                        triggeredAt = now,
                        triggeredPrice = price,
                        triggeredCurrency = currency,
                    )
                }
                AlertOutcome(updatedAlert = fired, notify = true)
            }
            !conditionMet && !alert.armed && alert.repeating -> {
                AlertOutcome(updatedAlert = alert.copy(armed = true), notify = false)
            }
            else -> null
        }
    }
}
