package ru.netology.nework.data.mapper

import ru.netology.nework.data.api.dto.PostDto
import ru.netology.nework.data.db.entity.PostEntity
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Post

fun PostDto.toEntity(): PostEntity {
    val likesCount = likeOwnerIds?.size ?: 0
    val publishedLong = try {
        published.toLongOrNull() ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
    return PostEntity(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        content = content,
        published = publishedLong,
        likes = likesCount,
        likedByMe = likedByMe,
        attachmentUrl = attachment?.url,
        attachmentType = attachment?.type,
        link = link
    )
}

fun PostEntity.toDomain(): Post {
    val attachment = if (attachmentUrl != null && attachmentType != null) {
        Attachment(attachmentUrl, AttachmentType.valueOf(attachmentType))
    } else null
    return Post(
        id = id,
        author = author,
        authorAvatar = authorAvatar,
        published = published,
        content = content,
        likedByMe = likedByMe,
        likes = likes,
        attachment = attachment,
        link = link,
        authorId = authorId
    )
}

fun PostDto.toDomain(): Post {
    val likesCount = likeOwnerIds?.size ?: 0
    val publishedLong = try {
        published.toLongOrNull() ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
    return Post(
        id = id,
        author = author,
        authorAvatar = authorAvatar,
        published = publishedLong,
        content = content,
        likedByMe = likedByMe,
        likes = likesCount,
        attachment = attachment?.let { Attachment(it.url, AttachmentType.valueOf(it.type)) },
        link = link,
        authorId = authorId
    )
}