package ru.netology.nework.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.netology.nework.data.api.ApiService
import ru.netology.nework.data.api.dto.CreateJobRequest
import ru.netology.nework.data.mapper.toDomain
import ru.netology.nework.domain.repository.UserRepository
import ru.netology.nework.model.Job
import ru.netology.nework.model.Post
import ru.netology.nework.model.User
import ru.netology.nework.model.UserDetail
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UserRepository {

    override suspend fun getUsers(): Result<List<User>> = try {
        val response = apiService.getUsers()
        if (response.isSuccessful) {
            val usersDto = response.body() ?: emptyList()
            Result.success(usersDto.map { it.toDomain() })
        } else {
            Result.failure(Exception("Failed to load users: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserById(id: Long): Result<User> = try {
        val response = apiService.getUserById(id)
        if (response.isSuccessful) {
            val userDto = response.body()
            if (userDto != null) {
                Result.success(userDto.toDomain())
            } else {
                Result.failure(Exception("User not found"))
            }
        } else {
            Result.failure(Exception("Failed to load user: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserWall(authorId: Long): Result<List<Post>> = try {
        val response = apiService.getUserWall(authorId)
        if (response.isSuccessful) {
            val postsDto = response.body() ?: emptyList()
            Result.success(postsDto.map { it.toDomain() })
        } else {
            Result.failure(Exception("Failed to load user posts: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserJobs(userId: Long): Result<List<Job>> = try {
        val response = apiService.getUserJobs(userId)
        if (response.isSuccessful) {
            val jobsDto = response.body() ?: emptyList()
            Result.success(jobsDto.map { it.toDomain() })
        } else {
            Result.success(emptyList())
        }
    } catch (e: Exception) {
        Result.success(emptyList())
    }

    override suspend fun getUserDetail(
        userId: Long,
        user: User?
    ): Result<UserDetail> = try {
        coroutineScope {
            val wallDeferred = async { getUserWall(userId) }
            val jobsDeferred = async { getUserJobs(userId) }

            val wallResult = wallDeferred.await()
            val jobsResult = jobsDeferred.await()

            val wallPosts = wallResult.getOrNull() ?: emptyList()
            val jobs = jobsResult.getOrNull() ?: emptyList()

            val userData = user ?: run {
                val userResult = getUserById(userId)
                userResult.getOrNull() ?: User(userId, "", "", null)
            }

            Result.success(
                UserDetail(
                    user = userData,
                    wallPosts = wallPosts,
                    jobs = jobs
                )
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createJob(job: Job): Result<Job> = try {
        val request = CreateJobRequest(
            id = 0,
            name = job.name,
            position = job.position,
            start = job.start.toString(),
            finish = job.finish?.toString(),
            link = job.link
        )

        val response = apiService.createJob(request)

        if (response.isSuccessful) {
            val jobDto = response.body()
            if (jobDto != null) {
                Result.success(jobDto.toDomain())
            } else {
                Result.failure(Exception("Empty server response"))
            }
        } else {
            Result.failure(Exception("Failed to create job: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteJob(jobId: Long): Result<Unit> = try {
        val response = apiService.deleteJob(jobId)
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to delete job: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}