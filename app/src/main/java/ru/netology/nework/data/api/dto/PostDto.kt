package ru.netology.nework.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoordinatesDto(
    @Json(name = "lat") val lat: Double,
    @Json(name = "long") val long: Double
)

@JsonClass(generateAdapter = true)
data class AttachmentDto(
    @Json(name = "url") val url: String,
    @Json(name = "type") val type: String // "IMAGE", "VIDEO", "AUDIO"
)

@JsonClass(generateAdapter = true)
data class UserPreviewDto(
    @Json(name = "name") val name: String,
    @Json(name = "avatar") val avatar: String?
)

@JsonClass(generateAdapter = true)
data class PostDto(
    @Json(name = "id") val id: Long,
    @Json(name = "authorId") val authorId: Long,
    @Json(name = "author") val author: String,
    @Json(name = "authorJob") val authorJob: String?,
    @Json(name = "authorAvatar") val authorAvatar: String?,
    @Json(name = "content") val content: String,
    @Json(name = "published") val published: String, // ISO 8601
    @Json(name = "coords") val coords: CoordinatesDto?,
    @Json(name = "link") val link: String?,
    @Json(name = "mentionIds") val mentionIds: List<Long>?,
    @Json(name = "mentionedMe") val mentionedMe: Boolean,
    @Json(name = "likeOwnerIds") val likeOwnerIds: List<Long>?,
    @Json(name = "likedByMe") val likedByMe: Boolean,
    @Json(name = "attachment") val attachment: AttachmentDto?,
    @Json(name = "users") val users: Map<Long, UserPreviewDto>?
)