package ru.netology.nework.domain.repository

import androidx.lifecycle.LiveData
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.Coordinates
import ru.netology.nework.model.Event
import ru.netology.nework.model.EventType
import java.time.LocalDateTime

interface EventRepository {

    fun getEventsLiveData(): LiveData<List<Event>>

    suspend fun getEvents(): Result<List<Event>>

    suspend fun getEventById(id: Long): Result<Event>

    suspend fun likeEvent(id: Long): Result<Event>

    suspend fun unlikeEvent(id: Long): Result<Event>

    suspend fun participateEvent(id: Long): Result<Event>

    suspend fun unparticipateEvent(id: Long): Result<Event>

    suspend fun deleteEvent(id: Long): Result<Unit>

    suspend fun saveEvent(
        content: String,
        attachment: Attachment?,
        coords: Coordinates?,
        eventType: EventType,
        eventDateTime: LocalDateTime,
        speakerIds: Set<Long>
    ): Result<Event>

    suspend fun updateEvent(
        id: Long,
        content: String,
        attachment: Attachment?,
        coords: Coordinates?,
        eventType: EventType,
        eventDateTime: LocalDateTime,
        speakerIds: Set<Long>
    ): Result<Event>
}