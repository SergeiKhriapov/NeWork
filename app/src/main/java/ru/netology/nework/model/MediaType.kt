package ru.netology.nework.model

enum class MediaType {
    IMAGE, VIDEO, AUDIO
}

data class MediaAttachment(
    val uri: String,
    val type: MediaType
)