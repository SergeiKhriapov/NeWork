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
    val lat: Double?,      // добавлено
    val lng: Double?       // добавлено
)