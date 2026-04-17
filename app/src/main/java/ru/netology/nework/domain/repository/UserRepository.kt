package ru.netology.nework.domain.repository

import ru.netology.nework.model.Job
import ru.netology.nework.model.Post
import ru.netology.nework.model.User
import ru.netology.nework.model.UserDetail

interface UserRepository {
    suspend fun getUsers(): Result<List<User>>
    suspend fun getUserById(id: Long): Result<User>
    suspend fun getUserWall(authorId: Long): Result<List<Post>>
    suspend fun getUserJobs(userId: Long): Result<List<Job>>
    suspend fun getUserDetail(userId: Long, user: User? = null): Result<UserDetail>
    suspend fun createJob(job: Job): Result<Job>
    suspend fun deleteJob(jobId: Long): Result<Unit>
}