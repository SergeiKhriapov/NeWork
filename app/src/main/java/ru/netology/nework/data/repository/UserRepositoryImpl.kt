package ru.netology.nework.data.repository

import android.util.Log
import ru.netology.nework.data.api.ApiService
import ru.netology.nework.data.mapper.toDomain
import ru.netology.nework.domain.repository.UserRepository
import ru.netology.nework.model.User
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepositoryImpl"

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UserRepository {

    override suspend fun getUsers(): Result<List<User>> = try {
        val response = apiService.getUsers()
        if (response.isSuccessful) {
            val usersDto = response.body() ?: emptyList()
            Log.d(TAG, "Received ${usersDto.size} users")
            Result.success(usersDto.map { it.toDomain() })
        } else {
            Log.e(TAG, "Error fetching users: ${response.code()}")
            Result.failure(Exception("Ошибка загрузки пользователей"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in getUsers", e)
        Result.failure(e)
    }
}