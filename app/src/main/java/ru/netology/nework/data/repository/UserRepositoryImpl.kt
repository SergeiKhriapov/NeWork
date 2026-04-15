package ru.netology.nework.data.repository

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.netology.nework.data.api.ApiService
import ru.netology.nework.data.api.dto.CreateJobRequest
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
            Log.d(TAG, "=== getUserJobs for userId: $userId ===")
            Log.d(TAG, "Received ${jobsDto.size} jobs")
            jobsDto.forEachIndexed { index, jobDto ->
                Log.d(TAG, "Job $index: id=${jobDto.id}, name=${jobDto.name}")
            }
            Result.success(jobsDto.map { it.toDomain() })
        } else {
            Log.w(TAG, "Error fetching jobs for user $userId: ${response.code()}, returning empty list")
            Result.success(emptyList())
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in getUserJobs", e)
        Result.success(emptyList())
    }

    override suspend fun getUserDetail(userId: Long, user: User?): Result<UserRepository.UserDetailData> = try {
        Log.d(TAG, "=== getUserDetail START for userId: $userId ===")

        coroutineScope {
            Log.d(TAG, "Starting parallel requests for userId: $userId")

            val wallDeferred = async { getUserWall(userId) }
            val jobsDeferred = async { getUserJobs(userId) }

            val wallResult = wallDeferred.await()
            val jobsResult = jobsDeferred.await()

            Log.d(TAG, "wallResult.isSuccess: ${wallResult.isSuccess}, jobsResult.isSuccess: ${jobsResult.isSuccess}")

            if (wallResult.isFailure) {
                Log.e(TAG, "wallResult failed: ${wallResult.exceptionOrNull()?.message}")
            }
            if (jobsResult.isFailure) {
                Log.e(TAG, "jobsResult failed: ${jobsResult.exceptionOrNull()?.message}")
            }

            val wallPosts = wallResult.getOrNull() ?: emptyList()
            val jobs = jobsResult.getOrNull() ?: emptyList()

            Log.d(TAG, "Final result: wallPosts=${wallPosts.size}, jobs=${jobs.size}")
            jobs.forEachIndexed { index, job ->
                Log.d(TAG, "Final Job $index: id=${job.id}, name=${job.name}")
            }

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
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in getUserDetail", e)
        Result.failure(e)
    }

    override suspend fun createJob(job: Job): Result<Job> = try {
        Log.d(TAG, "Creating job: name=${job.name}, position=${job.position}")

        val request = CreateJobRequest(
            id = 0,
            name = job.name,
            position = job.position,
            start = job.start.toString(),
            finish = job.finish?.toString(),
            link = job.link
        )

        val response = apiService.createJob(request)

        Log.d(TAG, "Response code: ${response.code()}")

        if (response.isSuccessful) {
            val jobDto = response.body()
            if (jobDto != null) {
                Log.d(TAG, "Job created successfully with id: ${jobDto.id}")
                Result.success(jobDto.toDomain())
            } else {
                Log.e(TAG, "Empty response body")
                Result.failure(Exception("Пустой ответ от сервера"))
            }
        } else {
            Log.e(TAG, "Error creating job: ${response.code()}, message: ${response.message()}")
            Result.failure(Exception("Ошибка создания работы: ${response.code()}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in createJob", e)
        Result.failure(e)
    }

    override suspend fun deleteJob(jobId: Long): Result<Unit> = try {
        Log.d(TAG, "Deleting job: $jobId")
        val response = apiService.deleteJob(jobId)
        if (response.isSuccessful) {
            Log.d(TAG, "Job deleted successfully")
            Result.success(Unit)
        } else {
            Log.e(TAG, "Error deleting job: ${response.code()}")
            Result.failure(Exception("Ошибка удаления работы: ${response.code()}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in deleteJob", e)
        Result.failure(e)
    }
}