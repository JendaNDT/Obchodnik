package cz.obchodnik.data.remote.coingecko

import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoMarketChartDto
import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoMarketCoinDto
import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinGeckoApi {
    @GET("search")
    suspend fun search(
        @Query("query") query: String,
    ): CoinGeckoSearchResponseDto

    @GET("coins/markets")
    suspend fun markets(
        @Query("vs_currency") vsCurrency: String,
        @Query("ids") ids: String,
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = true,
        @Query("price_change_percentage") priceChangePercentage: String = "24h,7d,30d",
    ): List<CoinGeckoMarketCoinDto>

    @GET("coins/{id}/market_chart")
    suspend fun marketChart(
        @Path("id") id: String,
        @Query("vs_currency") vsCurrency: String,
        @Query("days") days: String,
    ): CoinGeckoMarketChartDto

    @GET("coins/{id}/ohlc")
    suspend fun ohlc(
        @Path("id") id: String,
        @Query("vs_currency") vsCurrency: String,
        @Query("days") days: String,
    ): List<List<Double>>

    @GET("simple/price")
    suspend fun simplePrice(
        @Query("ids") ids: String,
        @Query("vs_currencies") vsCurrencies: String,
    ): Map<String, Map<String, Double>>
}
