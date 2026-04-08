package ru.netology.nework.data.api.dto

import java.time.OffsetDateTime

data class JobDto(
    val id: Long,
    val name: String,
    val position: String,
    val start: OffsetDateTime,
    val finish: OffsetDateTime?,
    val link: String?
)