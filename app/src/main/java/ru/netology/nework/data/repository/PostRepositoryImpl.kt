package ru.netology.nework.data.repository

import ru.netology.nework.data.api.ApiService
import ru.netology.nework.data.db.dao.PostDao
import ru.netology.nework.data.mapper.toDomain
import ru.netology.nework.data.mapper.toEntity
import ru.netology.nework.domain.repository.PostRepository
import ru.netology.nework.model.Post
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

private const val TAG = "PostRepositoryImpl"

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val postDao: PostDao
) : PostRepository {

    override suspend fun getPosts(): Result<List<Post>> = try {
        Log.d(TAG, "Fetching posts from network")
        val response = apiService.getPosts()
        if (response.isSuccessful) {
            val postsDto = response.body() ?: emptyList()
            Log.d(TAG, "Received ${postsDto.size} posts from network")
            val entities = postsDto.map { it.toEntity() }
            Log.d(TAG, "Inserting ${entities.size} posts into DB")
            postDao.insert(entities)
            Log.d(TAG, "Fetching posts from DB")
            val fromDb = postDao.getAll().map { it.toDomain() }
            Log.d(TAG, "Returning ${fromDb.size} posts from DB")
            Result.success(fromDb)
        } else {
            Log.e(TAG, "Network error: ${response.code()}")
            val cached = postDao.getAll().map { it.toDomain() }
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Returning ${cached.size} cached posts")
                Result.success(cached)
            } else {
                Result.failure(Exception("Нет данных и нет сети"))
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in getPosts", e)
        val cached = postDao.getAll().map { it.toDomain() }
        if (cached.isNotEmpty()) {
            Log.d(TAG, "Returning ${cached.size} cached posts after exception")
            Result.success(cached)
        } else {
            Result.failure(e)
        }
    }
}