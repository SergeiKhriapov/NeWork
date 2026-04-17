package ru.netology.nework.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.data.api.ApiService
import ru.netology.nework.data.api.dto.AttachmentDto
import ru.netology.nework.data.api.dto.CoordinatesDto
import ru.netology.nework.data.api.dto.CreateEventRequest
import ru.netology.nework.data.mapper.toDomain
import ru.netology.nework.domain.repository.EventRepository
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Coordinates
import ru.netology.nework.model.Event
import ru.netology.nework.model.EventType
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : EventRepository {

    private val _events = MutableLiveData<List<Event>>(emptyList())

    override fun getEventsLiveData(): LiveData<List<Event>> = _events

    override suspend fun getEvents(): Result<List<Event>> {
        return try {
            val response = apiService.getEvents()
            if (response.isSuccessful) {
                val events = response.body()?.map { it.toDomain() } ?: emptyList()
                _events.postValue(events)
                Result.success(events)
            } else {
                Result.failure(Exception("Failed to load events"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEventById(id: Long): Result<Event> {
        return try {
            val response = apiService.getEventById(id)
            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Event not found")
                Result.success(event)
            } else {
                Result.failure(Exception("Failed to load event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likeEvent(id: Long): Result<Event> {
        return try {
            val response = apiService.likeEvent(id)
            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Error")
                updateEventInList(event)
                Result.success(event)
            } else {
                Result.failure(Exception("Failed to like event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikeEvent(id: Long): Result<Event> {
        return try {
            val response = apiService.unlikeEvent(id)
            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Error")
                updateEventInList(event)
                Result.success(event)
            } else {
                Result.failure(Exception("Failed to unlike event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun participateEvent(id: Long): Result<Event> {
        return try {
            val response = apiService.participateEvent(id)
            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Error")
                updateEventInList(event)
                Result.success(event)
            } else {
                Result.failure(Exception("Failed to participate in event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unparticipateEvent(id: Long): Result<Event> {
        return try {
            val response = apiService.unparticipateEvent(id)
            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Error")
                updateEventInList(event)
                Result.success(event)
            } else {
                Result.failure(Exception("Failed to cancel participation"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(id: Long): Result<Unit> {
        return try {
            val response = apiService.deleteEvent(id)
            if (response.isSuccessful) {
                val currentList = _events.value ?: emptyList()
                _events.postValue(currentList.filter { it.id != id })
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveEvent(
        content: String,
        attachment: Attachment?,
        coords: Coordinates?,
        eventType: EventType,
        eventDateTime: LocalDateTime,
        speakerIds: Set<Long>
    ): Result<Event> {
        return try {
            var mediaUrl: String? = null
            var attachmentType: AttachmentType? = null

            if (attachment != null) {
                val file = File(attachment.url)
                if (!file.exists()) {
                    return Result.failure(Exception("File not found"))
                }

                attachmentType = attachment.type
                val mimeType = attachment.type.toMimeType()
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val uploadResponse = apiService.uploadMedia(part)
                if (!uploadResponse.isSuccessful) {
                    return Result.failure(Exception("Media upload failed"))
                }

                val mediaResponse = uploadResponse.body()
                mediaUrl = mediaResponse?.url ?: return Result.failure(Exception("No URL in media response"))
            }

            val attachmentDto = mediaUrl?.let { url ->
                AttachmentDto(url, attachmentType?.name ?: return Result.failure(Exception("No attachment type")))
            }

            val coordinatesDto = coords?.let {
                CoordinatesDto(lat = it.lat, lng = it.lng)
            }

            val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            val dateTimeStr = eventDateTime.atZone(java.time.ZoneId.systemDefault())
                .format(formatter)

            val request = CreateEventRequest(
                id = 0,
                content = content,
                datetime = dateTimeStr,
                type = eventType.name,
                attachment = attachmentDto,
                coords = coordinatesDto,
                speakerIds = speakerIds.toList()
            )

            val response = apiService.createEvent(request)

            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Empty response")
                val currentList = _events.value ?: emptyList()
                _events.postValue(listOf(event) + currentList)
                Result.success(event)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEvent(
        id: Long,
        content: String,
        attachment: Attachment?,
        coords: Coordinates?,
        eventType: EventType,
        eventDateTime: LocalDateTime,
        speakerIds: Set<Long>
    ): Result<Event> {
        return try {
            var mediaUrl: String? = null
            var attachmentType: AttachmentType? = null

            val isNewLocalFile = attachment != null &&
                    !attachment.url.startsWith("http://") &&
                    !attachment.url.startsWith("https://")

            if (isNewLocalFile) {
                val file = File(attachment.url)
                if (!file.exists()) {
                    return Result.failure(Exception("File not found"))
                }

                attachmentType = attachment.type
                val mimeType = attachment.type.toMimeType()
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val uploadResponse = apiService.uploadMedia(part)
                if (!uploadResponse.isSuccessful) {
                    return Result.failure(Exception("Media upload failed"))
                }

                val mediaResponse = uploadResponse.body()
                mediaUrl = mediaResponse?.url ?: return Result.failure(Exception("No URL in media response"))
            } else if (attachment != null) {
                mediaUrl = attachment.url
                attachmentType = attachment.type
            }

            val attachmentDto = mediaUrl?.let { url ->
                AttachmentDto(url, attachmentType?.name ?: return Result.failure(Exception("No attachment type")))
            }

            val coordinatesDto = coords?.let {
                CoordinatesDto(lat = it.lat, lng = it.lng)
            }

            val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            val dateTimeStr = eventDateTime.atZone(java.time.ZoneId.systemDefault())
                .format(formatter)

            val request = CreateEventRequest(
                id = id,
                content = content,
                datetime = dateTimeStr,
                type = eventType.name,
                attachment = attachmentDto,
                coords = coordinatesDto,
                speakerIds = speakerIds.toList()
            )

            val response = apiService.createEvent(request)

            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Empty response")
                updateEventInList(event)
                Result.success(event)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun updateEventInList(updatedEvent: Event) {
        val currentList = _events.value ?: emptyList()
        _events.postValue(currentList.map { if (it.id == updatedEvent.id) updatedEvent else it })
    }

    private fun AttachmentType.toMimeType(): String = when (this) {
        AttachmentType.IMAGE -> "image/*"
        AttachmentType.VIDEO -> "video/*"
        AttachmentType.AUDIO -> "audio/*"
    }
}