package ru.netology.nework.data.api.dto

data class CreatePostRequest(
    val content: String?,
    val attachment: AttachmentDto?
)