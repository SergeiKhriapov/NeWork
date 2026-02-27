package ru.netology.nework.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoordinatesDto(
    @Json(name = "lat") val lat: Double,
    @Json(name = "long") val lng: Double
)