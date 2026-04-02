package ru.netology.nework.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateEventRequest(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "content") val content: String,
    @Json(name = "datetime") val datetime: String,
    @Json(name = "type") val type: String,
    @Json(name = "attachment") val attachment: AttachmentDto? = null,
    @Json(name = "coords") val coords: CoordinatesDto? = null,
    @Json(name = "participantsIds") val participantsIds: List<Long> = emptyList()  // Добавляем
)