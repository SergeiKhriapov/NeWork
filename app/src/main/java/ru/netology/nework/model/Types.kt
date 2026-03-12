package ru.netology.nework.model

import java.time.OffsetDateTime

enum class AttachmentType { IMAGE, VIDEO, AUDIO }
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