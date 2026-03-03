package ru.netology.nework.data.db

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import ru.netology.nework.data.db.entity.UserPreviewEntity
import ru.netology.nework.model.AttachmentType

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // AttachmentType
    @TypeConverter
    fun fromAttachmentType(type: AttachmentType?): String? = type?.name

    @TypeConverter
    fun toAttachmentType(name: String?): AttachmentType? = name?.let { AttachmentType.valueOf(it) }

    // List<Long>
    @TypeConverter
    fun fromLongList(ids: List<Long>?): String? = ids?.joinToString(",")

    @TypeConverter
    fun toLongList(data: String?): List<Long>? =
        data?.split(",")?.mapNotNull { it.toLongOrNull() }

    // Map<Long, UserPreviewEntity>
    @TypeConverter
    fun fromUserPreviewMap(map: Map<Long, UserPreviewEntity>?): String? {
        val type = Types.newParameterizedType(Map::class.java, Long::class.javaObjectType, UserPreviewEntity::class.java)
        val adapter = moshi.adapter<Map<Long, UserPreviewEntity>>(type)
        return map?.let { adapter.toJson(it) }
    }

    @TypeConverter
    fun toUserPreviewMap(json: String?): Map<Long, UserPreviewEntity>? {
        if (json.isNullOrBlank()) return null
        val type = Types.newParameterizedType(Map::class.java, Long::class.javaObjectType, UserPreviewEntity::class.java)
        val adapter = moshi.adapter<Map<Long, UserPreviewEntity>>(type)
        return adapter.fromJson(json)
    }
}