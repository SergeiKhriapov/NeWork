package ru.netology.nework.data.db

import androidx.room.TypeConverter
import ru.netology.nework.model.AttachmentType

class Converters {
    @TypeConverter
    fun fromAttachmentType(type: AttachmentType?): String? = type?.name

    @TypeConverter
    fun toAttachmentType(name: String?): AttachmentType? = name?.let { AttachmentType.valueOf(it) }
}