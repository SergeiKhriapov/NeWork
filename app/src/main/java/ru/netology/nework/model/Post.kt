package ru.netology.nework.model

data class Post(
    val id: Long,
    val author: String,
    val authorAvatar: String?,
    val published: Long,
    val content: String,
    val likedByMe: Boolean = false,
    val likes: Int = 0,
    val attachment: Attachment? = null,
    val link: String? = null,
    val authorId: Long,
    val coords: Coordinates? = null  // новое поле
)

data class Attachment(val url: String, val type: AttachmentType)
enum class AttachmentType { IMAGE, VIDEO, AUDIO }