package cz.obchodnik.data.remote.alphavantage.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlphaVantageCommodityDto(
    @SerialName("name") val name: String? = null,
    @SerialName("interval") val interval: String? = null,
    @SerialName("unit") val unit: String? = null,
    @SerialName("data") val data: List<CommodityDataPointDto>? = null,
    @SerialName("Note") val note: String? = null,
    @SerialName("Information") val information: String? = null,
)

@Serializable
data class CommodityDataPointDto(
    @SerialName("date") val date: String,
    @SerialName("value") val value: String,
)

@Serializable
data class AlphaVantageSeriesDto(
    @SerialName("Meta Data") val metaData: AlphaVantageMetaDto? = null,
    @SerialName("Time Series (Daily)") val timeSeries: Map<String, AlphaVantageBarDto>? = null,
    @SerialName("Note") val note: String? = null,
    @SerialName("Information") val information: String? = null,
)

@Serializable
data class AlphaVantageMetaDto(
    @SerialName("1. Information") val information: String? = null,
    @SerialName("2. Symbol") val symbol: String,
    @SerialName("3. Last Refreshed") val lastRefreshed: String,
    @SerialName("4. Output Size") val outputSize: String? = null,
    @SerialName("5. Time Zone") val timeZone: String? = null,
)

@Serializable
data class AlphaVantageBarDto(
    @SerialName("1. open") val open: String,
    @SerialName("2. high") val high: String,
    @SerialName("3. low") val low: String,
    @SerialName("4. close") val close: String,
    @SerialName("5. volume") val volume: String,
)
