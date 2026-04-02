package ru.netology.nework.data.repository

import android.util.Log
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

private const val TAG = "EventRepositoryImpl"


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
                Result.failure(Exception("Ошибка загрузки событий"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEventById(id: Long): Result<Event> {
        return try {
            val response = apiService.getEventById(id)
            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Событие не найдено")
                Result.success(event)
            } else {
                Result.failure(Exception("Ошибка загрузки события"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likeEvent(id: Long): Result<Event> {
        return try {
            val response = apiService.likeEvent(id)
            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Ошибка")
                updateEventInList(event)
                Result.success(event)
            } else {
                Result.failure(Exception("Ошибка лайка"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikeEvent(id: Long): Result<Event> {
        return try {
            val response = apiService.unlikeEvent(id)
            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Ошибка")
                updateEventInList(event)
                Result.success(event)
            } else {
                Result.failure(Exception("Ошибка снятия лайка"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun participateEvent(id: Long): Result<Event> {
        return try {
            val response = apiService.participateEvent(id)
            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Ошибка")
                updateEventInList(event)
                Result.success(event)
            } else {
                Result.failure(Exception("Ошибка участия"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unparticipateEvent(id: Long): Result<Event> {
        return try {
            val response = apiService.unparticipateEvent(id)
            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Ошибка")
                updateEventInList(event)
                Result.success(event)
            } else {
                Result.failure(Exception("Ошибка отмены участия"))
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
                Result.failure(Exception("Ошибка удаления"))
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
        participantIds: Set<Long>
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

            Log.d("EventRepositoryImpl", "Sending event: content=$content, datetime=$dateTimeStr, type=${eventType.name}, participants=${participantIds.size}")

            val request = CreateEventRequest(
                id = 0,
                content = content,
                datetime = dateTimeStr,
                type = eventType.name,
                attachment = attachmentDto,
                coords = coordinatesDto,
                participantsIds = participantIds.toList()
            )

            val response = apiService.createEvent(request)

            Log.d("EventRepositoryImpl", "Response code: ${response.code()}")
            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Empty response")
                val currentList = _events.value ?: emptyList()
                _events.postValue(listOf(event) + currentList)
                Log.d("EventRepositoryImpl", "Event saved successfully, new list size: ${_events.value?.size}")
                Result.success(event)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("EventRepositoryImpl", "Server error: ${response.code()}, body: $errorBody")
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("EventRepositoryImpl", "Exception in saveEvent", e)
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
        participantIds: Set<Long>
    ): Result<Event> {
        Log.d(TAG, "=== updateEvent IN REPOSITORY ===")
        Log.d(TAG, "id=$id, content=$content, eventType=${eventType.name}, eventDateTime=$eventDateTime")

        return try {
            var mediaUrl: String? = null
            var attachmentType: AttachmentType? = null

            // Проверяем, нужно ли загружать новое медиа
            val isNewLocalFile = attachment != null &&
                    !attachment.url.startsWith("http://") &&
                    !attachment.url.startsWith("https://")

            if (isNewLocalFile) {
                Log.d(TAG, "Uploading new media...")
                val file = File(attachment.url)
                if (!file.exists()) {
                    Log.e(TAG, "File not found: ${attachment.url}")
                    return Result.failure(Exception("File not found"))
                }

                attachmentType = attachment.type
                val mimeType = attachment.type.toMimeType()
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val uploadResponse = apiService.uploadMedia(part)
                if (!uploadResponse.isSuccessful) {
                    Log.e(TAG, "Media upload failed")
                    return Result.failure(Exception("Media upload failed"))
                }

                val mediaResponse = uploadResponse.body()
                mediaUrl = mediaResponse?.url ?: return Result.failure(Exception("No URL in media response"))
                Log.d(TAG, "Media uploaded: $mediaUrl")
            } else if (attachment != null) {
                // Существующее вложение с сервера - используем его URL
                mediaUrl = attachment.url
                attachmentType = attachment.type
                Log.d(TAG, "Using existing media: $mediaUrl")
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

            Log.d(TAG, "Sending request with datetime=$dateTimeStr")

            val request = CreateEventRequest(
                id = id,
                content = content,
                datetime = dateTimeStr,
                type = eventType.name,
                attachment = attachmentDto,
                coords = coordinatesDto,
                participantsIds = participantIds.toList()
            )

            val response = apiService.createEvent(request)
            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                val event = response.body()?.toDomain() ?: throw Exception("Empty response")
                Log.d(TAG, "Event updated successfully: id=${event.id}")
                updateEventInList(event)
                Result.success(event)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Server error: ${response.code()}, body: $errorBody")
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateEvent exception", e)
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