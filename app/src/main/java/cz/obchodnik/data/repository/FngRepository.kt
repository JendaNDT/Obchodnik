package cz.obchodnik.data.repository

import cz.obchodnik.core.Result
import cz.obchodnik.data.remote.fng.FngApi
import cz.obchodnik.data.remote.fng.dto.FngDataDto
import cz.obchodnik.domain.model.Fng
import java.util.concurrent.TimeUnit

class FngRepository(
    private val api: FngApi,
) {
    private var cachedCurrent: Fng? = null
    private var cachedCurrentTimestamp: Long = 0L

    private var cachedHistory: List<Fng>? = null
    private var cachedHistoryTimestamp: Long = 0L
    private var cachedHistoryLimit: Int = 0

    suspend fun getCurrentFng(): Result<Fng> {
        val now = System.currentTimeMillis()
        val cacheAge = now - cachedCurrentTimestamp
        val freshLimit = TimeUnit.MINUTES.toMillis(10)

        val cached = cachedCurrent
        if (cached != null && cacheAge < freshLimit) {
            return Result.Success(cached)
        }

        return runCatching {
            val response = api.getFearAndGreed(limit = 1)
            val dataDto = response.data?.firstOrNull() ?: throw IllegalStateException("FNG API returned empty data")
            dataDto.toDomain()
        }.fold(
            onSuccess = { fng ->
                cachedCurrent = fng
                cachedCurrentTimestamp = now
                Result.Success(fng)
            },
            onFailure = { error ->
                val fallback = cachedCurrent
                if (fallback != null) Result.Success(fallback) else Result.Error("Nepodařilo se stáhnout index Fear & Greed", error)
            }
        )
    }

    suspend fun getFngHistory(limit: Int): Result<List<Fng>> {
        val now = System.currentTimeMillis()
        val cacheAge = now - cachedHistoryTimestamp
        val freshLimit = TimeUnit.MINUTES.toMillis(10)

        val cached = cachedHistory
        if (cached != null && cacheAge < freshLimit && cachedHistoryLimit >= limit) {
            return Result.Success(cached.take(limit))
        }

        return runCatching {
            val response = api.getFearAndGreed(limit = limit)
            response.data.orEmpty().map { it.toDomain() }
        }.fold(
            onSuccess = { history ->
                cachedHistory = history
                cachedHistoryTimestamp = now
                cachedHistoryLimit = limit
                Result.Success(history)
            },
            onFailure = { error ->
                val fallback = cachedHistory
                if (fallback != null) Result.Success(fallback.take(limit)) else Result.Error("Nepodařilo se stáhnout historii Fear & Greed", error)
            }
        )
    }

    private fun FngDataDto.toDomain(): Fng {
        val valInt = value.toIntOrNull() ?: 50
        val timeLong = (timestamp.toLongOrNull() ?: 0L) * 1000L
        return Fng(
            value = valInt,
            classification = valueClassification,
            timestamp = timeLong
        )
    }
}
