package ru.netology.nework.model

import java.time.OffsetDateTime

data class Comment(
    val id: Long,
    val postId: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String?,
    val content: String,
    val published: OffsetDateTime,
    val likeOwnerIds: Set<Long>,
    val likedByMe: Boolean
) {
    val likes: Int get() = likeOwnerIds.size
}