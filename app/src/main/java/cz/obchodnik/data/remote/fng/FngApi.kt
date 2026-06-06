package cz.obchodnik.data.remote.fng

import cz.obchodnik.data.remote.fng.dto.FngResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface FngApi {
    @GET("fng/")
    suspend fun getFearAndGreed(
        @Query("limit") limit: Int = 1,
        @Query("format") format: String = "json",
    ): FngResponseDto
}
