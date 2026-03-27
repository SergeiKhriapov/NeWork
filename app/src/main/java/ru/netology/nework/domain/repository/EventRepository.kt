package ru.netology.nework.domain.repository

import androidx.lifecycle.LiveData
import ru.netology.nework.model.Event

interface EventRepository {
    suspend fun getEvents(): Result<List<Event>>
    fun getEventsLiveData(): LiveData<List<Event>>
    suspend fun getEventById(id: Long): Result<Event>
    suspend fun likeEvent(id: Long): Result<Event>
    suspend fun unlikeEvent(id: Long): Result<Event>
    suspend fun participateEvent(id: Long): Result<Event>
    suspend fun unparticipateEvent(id: Long): Result<Event>
    suspend fun deleteEvent(id: Long): Result<Unit>  // Добавлен метод удаления
}