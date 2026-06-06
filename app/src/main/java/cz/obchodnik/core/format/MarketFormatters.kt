package cz.obchodnik.core.format

import java.text.NumberFormat
import java.util.Locale

object MarketFormatters {
    private val cs = Locale("cs", "CZ")

    fun price(value: Double?, currency: String): String {
        if (value == null) return "—"
        val decimals = when {
            value >= 1_000 -> 0
            value >= 1 -> 2
            value >= 0.1 -> 4
            else -> 5
        }
        val formatted = NumberFormat.getNumberInstance(cs).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }.format(value)
        return if (currency.lowercase() == "czk") "$formatted Kč" else "$$formatted"
    }

    fun percent(value: Double?): String {
        if (value == null) return "—"
        val sign = if (value >= 0) "+" else "-"
        val body = NumberFormat.getNumberInstance(cs).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(kotlin.math.abs(value))
        return "$sign$body %"
    }
}
