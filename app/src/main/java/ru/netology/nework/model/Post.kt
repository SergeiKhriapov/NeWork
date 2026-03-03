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
    val attachment: Attachment? = null,
    val link: String? = null,
    val coords: Coordinates? = null,
    val mentionIds: List<Long> = emptyList(),       // добавлено
    val mentionedMe: Boolean = false,               // добавлено
    val users: Map<Long, UserPreview>? = null       // добавлено
)

data class Attachment(val url: String, val type: AttachmentType)
enum class AttachmentType { IMAGE, VIDEO, AUDIO }

data class UserPreview(val name: String, val avatar: String?)