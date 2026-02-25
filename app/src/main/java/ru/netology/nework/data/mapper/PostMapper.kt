package ru.netology.nework.data.mapper

import ru.netology.nework.data.api.dto.PostDto
import ru.netology.nework.data.db.entity.PostEntity
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Post

fun PostDto.toEntity(): PostEntity {
    // Количество лайков = размер списка likeOwnerIds (если не null, иначе 0)
    val likesCount = likeOwnerIds?.size ?: 0

    // Преобразуем дату: предположим, что сервер отдаёт строку в ISO формате,
    // но пока для совместимости оставляем попытку преобразовать в Long.
    // Если не получается, ставим текущее время.
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
        likes = likesCount,  // ← используем вычисленное значение
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