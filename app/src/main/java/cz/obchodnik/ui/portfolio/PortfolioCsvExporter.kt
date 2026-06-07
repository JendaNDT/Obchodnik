package cz.obchodnik.ui.portfolio

import java.util.Locale

object PortfolioCsvExporter {
    private val headers = listOf(
        "Symbol",
        "Název",
        "Množství",
        "Nákupní cena",
        "Aktuální cena",
        "Hodnota",
        "Investováno",
        "P/L",
        "P/L %",
        "Měna",
    )

    fun export(state: PortfolioUiState): String {
        val rows = buildList {
            add(headers)
            state.items.forEach { item ->
                add(
                    listOf(
                        item.asset.symbol,
                        item.asset.name,
                        number(item.holding.qty),
                        number(item.holding.avgPrice),
                        number(item.quote?.price ?: item.holding.avgPrice),
                        number(item.value),
                        number(item.invested),
                        number(item.plValue),
                        item.plPct?.let(::number).orEmpty(),
                        state.currency.uppercase(Locale.US),
                    ),
                )
            }
        }
        return rows.joinToString(separator = "\n", postfix = "\n") { row ->
            row.joinToString(separator = ",", transform = ::cell)
        }
    }

    private fun number(value: Double): String {
        val raw = String.format(Locale.US, "%.8f", value)
        return raw.trimEnd('0').trimEnd('.').ifEmpty { "0" }
    }

    private fun cell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
