package ru.netology.nework.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nework.data.api.ApiService
import ru.netology.nework.data.db.dao.EventDao
import ru.netology.nework.data.db.entity.EventEntity
import ru.netology.nework.data.mapper.toDomain
import ru.netology.nework.data.mapper.toEntity
import ru.netology.nework.domain.repository.EventRepository
import ru.netology.nework.model.Event
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "EventRepositoryImpl"

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val eventDao: EventDao
) : EventRepository {

    override fun getEventsLiveData(): LiveData<List<Event>> {
        return eventDao.getAllLiveData().map { entities ->
            entities.map { entity ->
                entity.toDomain()
            }
        }
    }

    override suspend fun getEvents(): Result<List<Event>> = try {
        Log.d(TAG, "Fetching events from network")
        val response = apiService.getEvents()
        if (response.isSuccessful) {
            val eventsDto = response.body() ?: emptyList()
            Log.d(TAG, "Received ${eventsDto.size} events from network")
            val entities = eventsDto.map { dto ->
                dto.toEntity()
            }
            eventDao.insert(entities)
            val fromDb = eventDao.getAll().map { entity ->
                entity.toDomain()
            }
            Result.success(fromDb)
        } else {
            Log.e(TAG, "Network error: ${response.code()}")
            val cached = eventDao.getAll().map { entity ->
                entity.toDomain()
            }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(Exception("Нет данных и нет сети"))
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in getEvents", e)
        val cached = eventDao.getAll().map { entity ->
            entity.toDomain()
        }
        if (cached.isNotEmpty()) {
            Result.success(cached)
        } else {
            Result.failure(e)
        }
    }

    override suspend fun getEventById(id: Long): Result<Event> = try {
        val response = apiService.getEventById(id)
        if (response.isSuccessful) {
            val eventDto = response.body() ?: return Result.failure(Exception("Пустой ответ"))
            eventDao.insert(listOf(eventDto.toEntity()))
            Result.success(eventDto.toDomain())
        } else {
            val cached = eventDao.getById(id)?.toDomain()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(Exception("Событие не найдено"))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun likeEvent(id: Long): Result<Event> = try {
        val response = apiService.likeEvent(id)
        if (response.isSuccessful) {
            val eventDto = response.body() ?: return Result.failure(Exception("Пустой ответ"))
            eventDao.insert(listOf(eventDto.toEntity()))
            Result.success(eventDto.toDomain())
        } else {
            val errorMsg = when (response.code()) {
                403 -> "Нужно авторизоваться"
                404 -> "Событие не найдено"
                else -> "Ошибка при лайке: ${response.code()}"
            }
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun unlikeEvent(id: Long): Result<Event> = try {
        val response = apiService.unlikeEvent(id)
        if (response.isSuccessful) {
            val eventDto = response.body() ?: return Result.failure(Exception("Пустой ответ"))
            eventDao.insert(listOf(eventDto.toEntity()))
            Result.success(eventDto.toDomain())
        } else {
            val errorMsg = when (response.code()) {
                403 -> "Нужно авторизоваться"
                404 -> "Событие не найдено"
                else -> "Ошибка при снятии лайка: ${response.code()}"
            }
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun participateEvent(id: Long): Result<Event> = try {
        val response = apiService.participateEvent(id)
        if (response.isSuccessful) {
            val eventDto = response.body() ?: return Result.failure(Exception("Пустой ответ"))
            eventDao.insert(listOf(eventDto.toEntity()))
            Result.success(eventDto.toDomain())
        } else {
            val errorMsg = when (response.code()) {
                403 -> "Нужно авторизоваться"
                404 -> "Событие не найдено"
                else -> "Ошибка при участии: ${response.code()}"
            }
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun unparticipateEvent(id: Long): Result<Event> = try {
        val response = apiService.unparticipateEvent(id)
        if (response.isSuccessful) {
            val eventDto = response.body() ?: return Result.failure(Exception("Пустой ответ"))
            eventDao.insert(listOf(eventDto.toEntity()))
            Result.success(eventDto.toDomain())
        } else {
            val errorMsg = when (response.code()) {
                403 -> "Нужно авторизоваться"
                404 -> "Событие не найдено"
                else -> "Ошибка при отказе от участия: ${response.code()}"
            }
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteEvent(id: Long): Result<Unit> = try {
        Log.d(TAG, "Deleting event $id")
        val response = apiService.deleteEvent(id)
        if (response.isSuccessful) {
            eventDao.deleteById(id)
            Log.d(TAG, "Deleted event $id")
            Result.success(Unit)
        } else {
            Log.e(TAG, "Error deleting event: ${response.code()}")
            val errorMsg = when (response.code()) {
                403 -> "Нужно авторизоваться"
                404 -> "Событие не найдено"
                else -> "Ошибка удаления: ${response.code()}"
            }
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in deleteEvent", e)
        Result.failure(e)
    }
}