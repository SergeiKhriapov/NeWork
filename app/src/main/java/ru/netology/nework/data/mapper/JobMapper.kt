package ru.netology.nework.data.mapper

import ru.netology.nework.data.api.dto.JobDto
import ru.netology.nework.model.Job
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

fun JobDto.toDomain(): Job = Job(
    id = id,
    name = name,
    position = position,
    start = if (start is String) OffsetDateTime.parse(start, formatter) else start as OffsetDateTime,
    finish = if (finish != null) {
        if (finish is String) OffsetDateTime.parse(finish, formatter) else finish as OffsetDateTime
    } else null,
    link = link
)

fun Job.toDto(): JobDto = JobDto(
    id = id,
    name = name,
    position = position,
    start = start,
    finish = finish,
    link = link
)