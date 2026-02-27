package ru.netology.nework.model

enum class MediaType {
    IMAGE, VIDEO, AUDIO
}

data class MediaAttachment(
    val uri: String,      // строковое представление URI (можно хранить как String)
    val type: MediaType
)