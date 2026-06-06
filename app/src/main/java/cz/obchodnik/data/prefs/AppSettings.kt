package cz.obchodnik.data.prefs

data class AppSettings(
    val theme: String = "terminal",
    val accent: String = "blue",
    val currency: String = "usd",
    val refreshIntervalMinutes: Int = 30,
    val defaultChart: String = "line",
    val density: String = "normal",
    val showFngOnWidget: Boolean = true,
    val coingeckoKey: String = "",
    val alphaVantageKey: String = "",
    val onboardingDone: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val avDailyCount: Int = 0,
    val avCountDate: String = "",
    val usdCzkRate: Double = 23.0,
    val usdCzkRateLastUpdated: Long = 0L,
)
