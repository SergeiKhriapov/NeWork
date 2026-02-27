package ru.netology.nework.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.data.api.ApiService
import ru.netology.nework.data.api.dto.AttachmentDto
import ru.netology.nework.data.api.dto.CreatePostRequest
import ru.netology.nework.data.db.dao.PostDao
import ru.netology.nework.data.mapper.toDomain
import ru.netology.nework.data.mapper.toEntity
import ru.netology.nework.domain.repository.PostRepository
import ru.netology.nework.model.MediaAttachment
import ru.netology.nework.model.MediaType
import ru.netology.nework.model.Post
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PostRepositoryImpl"

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val postDao: PostDao
) : PostRepository {

    override fun getPostsLiveData(): LiveData<List<Post>> {
        return postDao.getAllLiveData().map { entities ->
            Log.d(TAG, "LiveData emitted ${entities.size} posts")
            if (entities.isNotEmpty()) {
                val firstDates = entities.take(3).joinToString { it.published.toString() }
                Log.d(TAG, "First 3 published dates: $firstDates")
            }
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPosts(): Result<List<Post>> = try {
        Log.d(TAG, "Fetching posts from network")
        val response = apiService.getPosts()
        if (response.isSuccessful) {
            val postsDto = response.body() ?: emptyList()
            Log.d(TAG, "Received ${postsDto.size} posts from network")
            val entities = postsDto.map { it.toEntity() }
            Log.d(TAG, "Inserting ${entities.size} posts into DB")
            postDao.insert(entities)
            // После вставки LiveData автоматически обновится
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

    override suspend fun likePost(id: Long): Result<Post> = try {
        val response = apiService.likePost(id)
        if (response.isSuccessful) {
            val postDto = response.body() ?: return Result.failure(Exception("Пустой ответ"))
            postDao.insert(listOf(postDto.toEntity()))
            Log.d(TAG, "Updated post ${postDto.id} after like")
            Result.success(postDto.toDomain())
        } else {
            val errorMsg = when (response.code()) {
                403 -> "Нужно авторизоваться"
                404 -> "Пост не найден"
                else -> "Ошибка ${response.code()}"
            }
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun unlikePost(id: Long): Result<Post> = try {
        val response = apiService.unlikePost(id)
        if (response.isSuccessful) {
            val postDto = response.body() ?: return Result.failure(Exception("Пустой ответ"))
            postDao.insert(listOf(postDto.toEntity()))
            Log.d(TAG, "Updated post ${postDto.id} after unlike")
            Result.success(postDto.toDomain())
        } else {
            val errorMsg = when (response.code()) {
                403 -> "Нужно авторизоваться"
                404 -> "Пост не найден"
                else -> "Ошибка ${response.code()}"
            }
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun savePost(content: String?, attachment: MediaAttachment?): Result<Unit> {
        return try {
            var mediaUrl: String? = null

            if (attachment != null) {
                val file = File(attachment.uri)
                if (!file.exists()) {
                    return Result.failure(Exception("File not found"))
                }

                val mimeType = attachment.type.toMimeType()
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val uploadResponse = apiService.uploadMedia(part)
                if (!uploadResponse.isSuccessful) {
                    Log.e(TAG, "Media upload failed: ${uploadResponse.code()}")
                    return Result.failure(Exception("Media upload failed"))
                }

                val mediaResponse = uploadResponse.body()
                if (mediaResponse == null) {
                    return Result.failure(Exception("Empty media response"))
                }
                mediaUrl = mediaResponse.url
                if (mediaUrl == null) {
                    return Result.failure(Exception("No URL in media response"))
                }
            }

            val attachmentDto = mediaUrl?.let { url ->
                AttachmentDto(url, attachment!!.type.name)
            }

            val request = CreatePostRequest(content, attachmentDto)
            val response = apiService.createPost(request)

            if (response.isSuccessful) {
                val postDto = response.body()
                if (postDto != null) {
                    postDao.insert(listOf(postDto.toEntity()))
                    Log.d(TAG, "Post created successfully: ${postDto.id}")
                }
                Result.success(Unit)
            } else {
                Log.e(TAG, "Error creating post: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in savePost", e)
            Result.failure(e)
        }
    }

    override suspend fun deletePost(id: Long): Result<Unit> = try {
        val response = apiService.deletePost(id)
        if (response.isSuccessful) {
            postDao.deleteById(id)
            Log.d(TAG, "Deleted post $id")
            Result.success(Unit)
        } else {
            Log.e(TAG, "Error deleting post: ${response.code()}")
            Result.failure(Exception("Error ${response.code()}"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception in deletePost", e)
        Result.failure(e)
    }

    override suspend fun updatePost(id: Long, content: String?, attachment: MediaAttachment?): Result<Post> {
        return try {
            var mediaUrl: String? = null

            if (attachment != null) {
                val file = File(attachment.uri)
                if (!file.exists()) {
                    return Result.failure(Exception("File not found"))
                }

                val mimeType = attachment.type.toMimeType()
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val uploadResponse = apiService.uploadMedia(part)
                if (!uploadResponse.isSuccessful) {
                    Log.e(TAG, "Media upload failed: ${uploadResponse.code()}")
                    return Result.failure(Exception("Media upload failed"))
                }

                val mediaResponse = uploadResponse.body()
                if (mediaResponse == null) {
                    return Result.failure(Exception("Empty media response"))
                }
                mediaUrl = mediaResponse.url
                if (mediaUrl == null) {
                    return Result.failure(Exception("No URL in media response"))
                }
            }

            val attachmentDto = mediaUrl?.let { url ->
                AttachmentDto(url, attachment!!.type.name)
            }

            val request = CreatePostRequest(content, attachmentDto)
            val response = apiService.updatePost(id, request)

            if (response.isSuccessful) {
                val postDto = response.body()
                if (postDto != null) {
                    postDao.insert(listOf(postDto.toEntity()))
                    Log.d(TAG, "Updated post ${postDto.id}")
                    Result.success(postDto.toDomain())
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Log.e(TAG, "Error updating post: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in updatePost", e)
            Result.failure(e)
        }
    }

    private fun MediaType.toMimeType(): String = when (this) {
        MediaType.IMAGE -> "image/*"
        MediaType.VIDEO -> "video/*"
        MediaType.AUDIO -> "audio/*"
    }
}