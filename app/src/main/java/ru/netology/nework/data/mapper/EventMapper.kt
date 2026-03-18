package ru.netology.nework.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nework.api.dto.EventDto
import ru.netology.nework.data.db.entity.EventEntity
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Coordinates
import ru.netology.nework.model.Event
import ru.netology.nework.model.EventType
import ru.netology.nework.model.UserPreview
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val gson = Gson()
private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

fun EventDto.toEntity(): EventEntity {
    val likeOwnerIdsStr = likeOwnerIds?.joinToString(",")
    val speakerIdsStr = speakerIds?.joinToString(",")
    val participantsIdsStr = participantsIds?.joinToString(",")
    val usersJson = users?.let { gson.toJson(it) }

    return EventEntity(
        id = id,
        authorId = authorId,
        author = author,
        authorJob = authorJob,
        authorAvatar = authorAvatar,
        content = content,
        datetime = datetime,
        published = published,
        lat = coords?.lat,
        lng = coords?.lng,
        type = type,
        likeOwnerIds = likeOwnerIdsStr,
        likedByMe = likedByMe,
        speakerIds = speakerIdsStr,
        participantsIds = participantsIdsStr,
        participatedByMe = participatedByMe,
        attachmentUrl = attachment?.url,
        attachmentType = attachment?.type,
        link = link,
        users = usersJson
    )
}

fun EventEntity.toDomain(): Event {
    val likeOwnerIds = likeOwnerIds?.split(",")?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    val speakerIds = speakerIds?.split(",")?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    val participantsIds = participantsIds?.split(",")?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()

    val users = users?.let {
        val type = object : TypeToken<Map<Long, UserPreview>>() {}.type
        gson.fromJson<Map<Long, UserPreview>>(it, type)
    }

    val attachment = if (attachmentUrl != null && attachmentType != null) {
        Attachment(attachmentUrl, AttachmentType.valueOf(attachmentType))
    } else null

    val coords = if (lat != null && lng != null) {
        Coordinates(lat, lng)
    } else null

    return Event(
        id = id,
        authorId = authorId,
        author = author,
        authorJob = authorJob,
        authorAvatar = authorAvatar,
        content = content,
        datetime = OffsetDateTime.parse(datetime, formatter),
        published = OffsetDateTime.parse(published, formatter),
        coords = coords,
        type = EventType.valueOf(type),
        likeOwnerIds = likeOwnerIds,
        likedByMe = likedByMe,
        speakerIds = speakerIds,
        participantsIds = participantsIds,
        participatedByMe = participatedByMe,
        attachment = attachment,
        link = link,
        users = users
    )
}

fun EventDto.toDomain(): Event {
    val likeOwnerIds = likeOwnerIds?.toSet() ?: emptySet()
    val speakerIds = speakerIds?.toSet() ?: emptySet()
    val participantsIds = participantsIds?.toSet() ?: emptySet()

    val coords = this.coords?.let {
        Coordinates(it.lat, it.lng)
    }
    val attachment = this.attachment?.let {
        Attachment(it.url, AttachmentType.valueOf(it.type))
    }
    val users = this.users?.mapValues {
        UserPreview(it.value.name, it.value.avatar)
    }

    return Event(
        id = id,
        authorId = authorId,
        author = author,
        authorJob = authorJob,
        authorAvatar = authorAvatar,
        content = content,
        datetime = OffsetDateTime.parse(datetime, formatter),
        published = OffsetDateTime.parse(published, formatter),
        coords = coords,
        type = EventType.valueOf(type),
        likeOwnerIds = likeOwnerIds,
        likedByMe = likedByMe,
        speakerIds = speakerIds,
        participantsIds = participantsIds,
        participatedByMe = participatedByMe,
        attachment = attachment,
        link = link,
        users = users
    )
}