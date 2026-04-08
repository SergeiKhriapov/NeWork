package ru.netology.nework.data.repository

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.netology.nework.data.api.ApiService
import ru.netology.nework.data.mapper.toDomain
import ru.netology.nework.domain.repository.UserRepository
import ru.netology.nework.model.Job
import ru.netology.nework.model.Post
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
            Result.failure(Exception("Ошибка загрузки пользователей: ${response.code()}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in getUsers", e)
        Result.failure(e)
    }

    override suspend fun getUserById(id: Long): Result<User> = try {
        val response = apiService.getUserById(id)
        if (response.isSuccessful) {
            val userDto = response.body()
            if (userDto != null) {
                Log.d(TAG, "Received user: ${userDto.id}")
                Result.success(userDto.toDomain())
            } else {
                Result.failure(Exception("Пользователь не найден"))
            }
        } else {
            Log.e(TAG, "Error fetching user $id: ${response.code()}")
            Result.failure(Exception("Ошибка загрузки пользователя: ${response.code()}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in getUserById", e)
        Result.failure(e)
    }

    override suspend fun getUserWall(authorId: Long): Result<List<Post>> = try {
        val response = apiService.getUserWall(authorId)
        if (response.isSuccessful) {
            val postsDto = response.body() ?: emptyList()
            Log.d(TAG, "Received ${postsDto.size} wall posts for user $authorId")
            Result.success(postsDto.map { it.toDomain() })
        } else {
            Log.e(TAG, "Error fetching wall for user $authorId: ${response.code()}")
            Result.failure(Exception("Ошибка загрузки постов пользователя: ${response.code()}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in getUserWall", e)
        Result.failure(e)
    }

    override suspend fun getUserJobs(userId: Long): Result<List<Job>> = try {
        val response = apiService.getUserJobs(userId)
        if (response.isSuccessful) {
            val jobsDto = response.body() ?: emptyList()
            Log.d(TAG, "Received ${jobsDto.size} jobs for user $userId")
            // Преобразуем JobDto в Job
            Result.success(jobsDto.map { it.toDomain() })
        } else {
            Log.e(TAG, "Error fetching jobs for user $userId: ${response.code()}")
            Result.failure(Exception("Ошибка загрузки мест работы: ${response.code()}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in getUserJobs", e)
        Result.failure(e)
    }

    override suspend fun getUserDetail(userId: Long, user: User?): Result<UserRepository.UserDetailData> = try {
        coroutineScope {
            val wallDeferred = async { getUserWall(userId) }
            val jobsDeferred = async { getUserJobs(userId) }

            val wallResult = wallDeferred.await()
            val jobsResult = jobsDeferred.await()

            if (wallResult.isSuccess && jobsResult.isSuccess) {
                val wallPosts = wallResult.getOrNull() ?: emptyList()
                val jobs = jobsResult.getOrNull() ?: emptyList()
                val userData = user ?: run {
                    val userResult = getUserById(userId)
                    userResult.getOrNull() ?: User(userId, "", "", null)
                }

                Result.success(
                    UserRepository.UserDetailData(
                        user = userData,
                        wallPosts = wallPosts,
                        jobs = jobs
                    )
                )
            } else {
                val errorMessage = buildString {
                    if (wallResult.isFailure) append("Ошибка загрузки постов. ")
                    if (jobsResult.isFailure) append("Ошибка загрузки работ.")
                    if (isEmpty()) append("Неизвестная ошибка")
                }
                Result.failure(Exception(errorMessage))
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in getUserDetail", e)
        Result.failure(e)
    }
}