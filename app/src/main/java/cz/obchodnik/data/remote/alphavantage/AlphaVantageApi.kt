package cz.obchodnik.data.remote.alphavantage

import cz.obchodnik.data.remote.alphavantage.dto.AlphaVantageCommodityDto
import cz.obchodnik.data.remote.alphavantage.dto.AlphaVantageSeriesDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AlphaVantageApi {
    @GET("query")
    suspend fun getCommodity(
        @Query("function") function: String,
        @Query("interval") interval: String = "daily",
        @Query("apikey") apiKey: String,
    ): AlphaVantageCommodityDto

    @GET("query")
    suspend fun getStockSeries(
        @Query("function") function: String = "TIME_SERIES_DAILY",
        @Query("symbol") symbol: String,
        @Query("outputsize") outputSize: String = "compact",
        @Query("apikey") apiKey: String,
    ): AlphaVantageSeriesDto
}
