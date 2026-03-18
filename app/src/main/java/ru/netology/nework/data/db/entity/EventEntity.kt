package ru.netology.nework.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ru.netology.nework.data.db.Converters

@Entity(tableName = "events")
@TypeConverters(Converters::class)
data class EventEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorJob: String?,
    val authorAvatar: String?,
    val content: String,
    val datetime: String,
    val published: String,
    val lat: Double?,
    val lng: Double?,
    val type: String,
    val likeOwnerIds: String?,
    val likedByMe: Boolean,
    val speakerIds: String?,
    val participantsIds: String?,
    val participatedByMe: Boolean,
    val attachmentUrl: String?,
    val attachmentType: String?,
    val link: String?,
    val users: String?
)