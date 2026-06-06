package cz.obchodnik.data.remote.coingecko.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoinGeckoSearchResponseDto(
    val coins: List<CoinGeckoSearchCoinDto> = emptyList(),
)

@Serializable
data class CoinGeckoSearchCoinDto(
    val id: String,
    val name: String,
    @SerialName("api_symbol")
    val apiSymbol: String? = null,
    val symbol: String,
    @SerialName("market_cap_rank")
    val marketCapRank: Int? = null,
    val thumb: String? = null,
    val large: String? = null,
)

@Serializable
data class CoinGeckoMarketCoinDto(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String? = null,
    @SerialName("current_price")
    val currentPrice: Double? = null,
    @SerialName("market_cap")
    val marketCap: Double? = null,
    @SerialName("market_cap_rank")
    val marketCapRank: Int? = null,
    @SerialName("total_volume")
    val totalVolume: Double? = null,
    @SerialName("high_24h")
    val high24h: Double? = null,
    @SerialName("low_24h")
    val low24h: Double? = null,
    @SerialName("price_change_percentage_24h")
    val priceChangePercentage24h: Double? = null,
    @SerialName("price_change_percentage_24h_in_currency")
    val priceChangePercentage24hInCurrency: Double? = null,
    @SerialName("price_change_percentage_7d_in_currency")
    val priceChangePercentage7dInCurrency: Double? = null,
    @SerialName("price_change_percentage_30d_in_currency")
    val priceChangePercentage30dInCurrency: Double? = null,
    @SerialName("sparkline_in_7d")
    val sparklineIn7d: CoinGeckoSparklineDto? = null,
    @SerialName("last_updated")
    val lastUpdated: String? = null,
)

@Serializable
data class CoinGeckoSparklineDto(
    val price: List<Double> = emptyList(),
)

@Serializable
data class CoinGeckoMarketChartDto(
    val prices: List<List<Double>> = emptyList(),
    @SerialName("market_caps")
    val marketCaps: List<List<Double>> = emptyList(),
    @SerialName("total_volumes")
    val totalVolumes: List<List<Double>> = emptyList(),
)
