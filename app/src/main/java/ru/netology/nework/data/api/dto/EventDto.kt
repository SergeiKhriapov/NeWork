package ru.netology.nework.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoordinatesDto(
    val lat: Double,
    @Json(name = "long") val lng: Double
)

@JsonClass(generateAdapter = true)
data class AttachmentDto(
    val url: String,
    val type: String
)

@JsonClass(generateAdapter = true)
data class UserPreviewDto(
    val name: String,
    val avatar: String?
)

@JsonClass(generateAdapter = true)
data class EventDto(
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorJob: String?,
    val authorAvatar: String?,
    val content: String,
    val datetime: String,
    val published: String,
    val coords: CoordinatesDto?,
    val type: String,
    val likeOwnerIds: List<Long>,
    val likedByMe: Boolean,
    val speakerIds: List<Long>,
    val participantsIds: List<Long>,
    val participatedByMe: Boolean,
    val attachment: AttachmentDto?,
    val link: String?,
    val users: Map<Long, UserPreviewDto>?
)

@JsonClass(generateAdapter = true)
data class CreateEventRequest(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "content") val content: String,
    @Json(name = "datetime") val datetime: String,
    @Json(name = "type") val type: String,
    @Json(name = "attachment") val attachment: AttachmentDto? = null,
    @Json(name = "coords") val coords: CoordinatesDto? = null
)