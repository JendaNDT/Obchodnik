package cz.obchodnik.domain.model

/** One recorded total portfolio value for a given day in a given currency. */
data class PortfolioSnapshot(
    val id: Long,
    val dayStartMillis: Long,
    val totalValue: Double,
    val totalInvested: Double,
    val currency: String,
)
