package ru.netology.nework.model

import java.time.OffsetDateTime

sealed class Content(
    open val id: Long,
    open val authorId: Long,
    open val author: String,
    open val authorAvatar: String?,
    open val content: String,
    open val published: OffsetDateTime,
    open val coords: Coordinates?,
    open val link: String?,
    open val attachment: Attachment?,
    open val likeOwnerIds: Set<Long>,
    open val likedByMe: Boolean,
    open val users: Map<Long, UserPreview>?
) {
    val likes: Int get() = likeOwnerIds.size
}