package cz.obchodnik.domain.model

enum class ChartRange(val coingeckoMarketChartDays: String, val coingeckoOhlcDays: String) {
    D1("1", "1"),
    W1("7", "7"),
    M1("30", "30"),
    Y1("365", "365"),
    ALL("max", "365"),
}
