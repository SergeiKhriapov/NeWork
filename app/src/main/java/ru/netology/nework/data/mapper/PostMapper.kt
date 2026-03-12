package ru.netology.nework.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nework.data.api.dto.PostDto
import ru.netology.nework.data.db.entity.PostEntity
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Coordinates
import ru.netology.nework.model.Post
import ru.netology.nework.model.UserPreview
import java.time.OffsetDateTime
import java.time.Instant
import java.time.ZoneOffset

private val gson = Gson()

fun PostDto.toEntity(): PostEntity {
    val likesCount = likeOwnerIds?.size ?: 0
    val publishedMillis = try {
        Instant.parse(published).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    val likeOwnerIdsStr = likeOwnerIds?.joinToString(",")
    val mentionIdsStr = mentionIds?.joinToString(",")
    val usersJson = users?.let { gson.toJson(it) }

    return PostEntity(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        content = content,
        published = publishedMillis,
        likes = likesCount,
        likedByMe = likedByMe,
        attachmentUrl = attachment?.url,
        attachmentType = attachment?.type,
        link = link,
        lat = coords?.lat,
        lng = coords?.lng,
        likeOwnerIds = likeOwnerIdsStr,
        mentionIds = mentionIdsStr,
        mentionedMe = mentionedMe,
        users = usersJson
    )
}

fun PostEntity.toDomain(): Post {
    val likeOwnerIds = likeOwnerIds?.split(",")?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    val mentionIds = mentionIds?.split(",")?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()

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

    return Post(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        content = content,  // <-- добавлен content
        published = OffsetDateTime.ofInstant(Instant.ofEpochMilli(published), ZoneOffset.UTC),
        coords = coords,
        link = link,
        attachment = attachment,
        likeOwnerIds = likeOwnerIds,
        likedByMe = likedByMe,
        users = users,
        authorJob = null,  // <-- authorJob после users
        mentionIds = mentionIds,
        mentionedMe = mentionedMe
    )
}

fun PostDto.toDomain(): Post {
    val publishedDateTime = try {
        OffsetDateTime.parse(published)
    } catch (e: Exception) {
        OffsetDateTime.now()
    }

    val coords = this.coords?.let { Coordinates(it.lat, it.lng) }
    val attachment = this.attachment?.let {
        Attachment(
            url = it.url,
            type = AttachmentType.valueOf(it.type)
        )
    }
    val users = this.users?.mapValues {
        UserPreview(
            name = it.value.name,
            avatar = it.value.avatar
        )
    }

    return Post(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        content = content,  // <-- добавлен content
        published = publishedDateTime,
        coords = coords,
        link = link,
        attachment = attachment,
        likeOwnerIds = likeOwnerIds?.toSet() ?: emptySet(),
        likedByMe = likedByMe,
        users = users,
        authorJob = authorJob,  // <-- authorJob после users
        mentionIds = mentionIds?.toSet() ?: emptySet(),
        mentionedMe = mentionedMe
    )
}