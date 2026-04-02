package ru.netology.nework.model

enum class AttachmentType {
    IMAGE,
    VIDEO,
    AUDIO;

    fun toMimeType(): String = when (this) {
        IMAGE -> "image/*"
        VIDEO -> "video/*"
        AUDIO -> "audio/*"
    }
}

enum class EventType { OFFLINE, ONLINE }

data class Coordinates(
    val lat: Double,
    val lng: Double
)

data class Attachment(
    val url: String,
    val type: AttachmentType
)

data class UserPreview(
    val name: String,
    val avatar: String?
)