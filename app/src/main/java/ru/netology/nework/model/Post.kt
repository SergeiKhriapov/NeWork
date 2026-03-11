package ru.netology.nework.model

data class Post(
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String?,
    val content: String,
    val published: Long,
    val likedByMe: Boolean = false,
    val likes: Int = 0,
    val likeOwnerIds: Set<Long> = emptySet(),   // добавлено
    val mentionIds: Set<Long> = emptySet(),
    val mentionedMe: Boolean = false,
    val attachment: Attachment? = null,
    val link: String? = null,
    val coords: Coordinates? = null,
    val users: Map<Long, UserPreview>? = null
)

data class Attachment(val url: String, val type: AttachmentType)
enum class AttachmentType { IMAGE, VIDEO, AUDIO }

data class UserPreview(val name: String, val avatar: String?)