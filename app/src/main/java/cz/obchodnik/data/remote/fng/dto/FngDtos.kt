package cz.obchodnik.data.remote.fng.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FngResponseDto(
    @SerialName("name") val name: String? = null,
    @SerialName("data") val data: List<FngDataDto>? = null,
)

@Serializable
data class FngDataDto(
    @SerialName("value") val value: String,
    @SerialName("value_classification") val valueClassification: String,
    @SerialName("timestamp") val timestamp: String,
    @SerialName("time_until_update") val timeUntilUpdate: String? = null,
)
