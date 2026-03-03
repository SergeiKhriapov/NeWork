package ru.netology.nework.data.mapper

import ru.netology.nework.data.api.dto.PostDto
import ru.netology.nework.data.db.entity.PostEntity
import ru.netology.nework.data.db.entity.UserPreviewEntity
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Coordinates
import ru.netology.nework.model.Post
import ru.netology.nework.model.UserPreview
import java.time.Instant

fun PostDto.toEntity(): PostEntity {
    val likesCount = likeOwnerIds?.size ?: 0
    val publishedMillis = try {
        Instant.parse(published).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
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
        mentionIds = mentionIds ?: emptyList(),
        mentionedMe = mentionedMe,
        users = users?.mapValues { UserPreviewEntity(it.value.name, it.value.avatar) }
    )
}

fun PostEntity.toDomain(): Post {
    val attachment = if (attachmentUrl != null && attachmentType != null) {
        Attachment(attachmentUrl, AttachmentType.valueOf(attachmentType))
    } else null
    val coords = if (lat != null && lng != null) {
        Coordinates(lat!!, lng!!)
    } else null
    val users = this.users?.mapValues { UserPreview(it.value.name, it.value.avatar) }
    return Post(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        published = published,
        content = content,
        likedByMe = likedByMe,
        likes = likes,
        attachment = attachment,
        link = link,
        coords = coords,
        mentionIds = mentionIds,
        mentionedMe = mentionedMe,
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
        attachment = attachment,
        link = link,
        coords = coords,
        mentionIds = mentionIds ?: emptyList(),
        mentionedMe = mentionedMe,
        users = users
    )
}