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
    val likeOwnerIds: String?,   // например "1,2,3"
    val mentionIds: String?,     // например "4,5,6"
    val mentionedMe: Boolean,
    val users: String?            // JSON строка вида {"1":{"name":"...","avatar":"..."}, ...}
)