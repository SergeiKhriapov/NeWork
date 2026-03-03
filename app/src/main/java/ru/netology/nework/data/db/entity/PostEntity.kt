package ru.netology.nework.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String?,
    val content: String,
    val published: Long,
    val likes: Int,
    val likedByMe: Boolean,
    val attachmentUrl: String?,
    val attachmentType: String?,
    val link: String?,
    val lat: Double?,
    val lng: Double?,
    val mentionIds: List<Long>,
    val mentionedMe: Boolean,
    val users: Map<Long, UserPreviewEntity>?
)

data class UserPreviewEntity(
    val name: String,
    val avatar: String?
)