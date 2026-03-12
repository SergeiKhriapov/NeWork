package ru.netology.nework.model

import java.time.OffsetDateTime

data class Event(
    override val id: Long,
    override val authorId: Long,
    override val author: String,
    override val authorAvatar: String?,
    override val content: String,
    override val published: OffsetDateTime,
    override val coords: Coordinates?,
    override val link: String?,
    override val attachment: Attachment?,
    override val likeOwnerIds: Set<Long>,
    override val likedByMe: Boolean,
    override val users: Map<Long, UserPreview>?,

    val authorJob: String?,
    val datetime: OffsetDateTime,
    val type: EventType,
    val speakerIds: Set<Long>,
    val participantsIds: Set<Long>,
    val participatedByMe: Boolean
) : Content(
    id, authorId, author, authorAvatar, content, published,
    coords, link, attachment, likeOwnerIds, likedByMe, users
)