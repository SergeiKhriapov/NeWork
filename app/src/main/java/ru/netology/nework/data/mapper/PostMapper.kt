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
import java.time.Instant

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
        published = published,
        content = content,
        likedByMe = likedByMe,
        likes = likes,
        likeOwnerIds = likeOwnerIds,
        mentionIds = mentionIds,
        mentionedMe = mentionedMe,
        attachment = attachment,
        link = link,
        coords = coords,
        users = users
    )
}

fun PostDto.toDomain(): Post {
    val likesCount = likeOwnerIds?.size ?: 0
    val publishedMillis = try {
        Instant.parse(published).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    val coords = this.coords?.let { Coordinates(it.lat, it.lng) }
    val attachment = this.attachment?.let { Attachment(it.url, AttachmentType.valueOf(it.type)) }
    val users = this.users?.mapValues { UserPreview(it.value.name, it.value.avatar) }

    return Post(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        published = publishedMillis,
        content = content,
        likedByMe = likedByMe,
        likes = likesCount,
        likeOwnerIds = likeOwnerIds?.toSet() ?: emptySet(),
        mentionIds = mentionIds?.toSet() ?: emptySet(),
        mentionedMe = mentionedMe,
        attachment = attachment,
        link = link,
        coords = coords,
        users = users
    )
}