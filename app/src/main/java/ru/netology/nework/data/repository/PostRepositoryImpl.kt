package ru.netology.nework.data.repository

import CreatePostRequest
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.data.api.ApiService
import ru.netology.nework.data.api.dto.AttachmentDto
import ru.netology.nework.data.api.dto.CoordinatesDto
import ru.netology.nework.data.db.dao.PostDao
import ru.netology.nework.data.mapper.toDomain
import ru.netology.nework.data.mapper.toEntity
import ru.netology.nework.domain.repository.PostRepository
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Coordinates
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

    private val _allPosts = MutableLiveData<List<Post>>(emptyList())
    private var currentPosts: List<Post> = emptyList()
    private var oldestPostId: Long? = null
    private var isLoadingMore = false
    private var hasMore = true

    override fun getPostsLiveData(): LiveData<List<Post>> = _allPosts

    override suspend fun getLatestPosts(count: Int): Result<List<Post>> {
        return try {
            Log.d(TAG, "Fetching latest $count posts from network")
            val response = apiService.getLatestPosts(count)
            if (response.isSuccessful) {
                val postsDto = response.body() ?: emptyList()
                val posts = postsDto.map { it.toDomain() }

                currentPosts = posts
                _allPosts.postValue(posts)

                val entities = postsDto.map { it.toEntity() }
                postDao.insert(entities)

                if (posts.isNotEmpty()) {
                    oldestPostId = posts.last().id
                    hasMore = posts.size == count
                }

                Log.d(TAG, "Loaded ${posts.size} posts")
                Result.success(posts)
            } else {
                Log.e(TAG, "Network error: ${response.code()}")
                val cached = postDao.getAll().map { it.toDomain() }
                if (cached.isNotEmpty()) {
                    currentPosts = cached
                    _allPosts.postValue(cached)
                    Result.success(cached)
                } else {
                    Result.failure(Exception("Нет данных и нет сети"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getLatestPosts", e)
            val cached = postDao.getAll().map { it.toDomain() }
            if (cached.isNotEmpty()) {
                currentPosts = cached
                _allPosts.postValue(cached)
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getPostsBefore(id: Long, count: Int): Result<List<Post>> {
        return try {
            if (isLoadingMore || !hasMore) {
                return Result.success(emptyList())
            }

            isLoadingMore = true
            Log.d(TAG, "Fetching posts before $id")
            val response = apiService.getPostsBefore(id, count)
            isLoadingMore = false

            if (response.isSuccessful) {
                val postsDto = response.body() ?: emptyList()
                val newPosts = postsDto.map { it.toDomain() }

                if (newPosts.isNotEmpty()) {
                    currentPosts = currentPosts + newPosts
                    _allPosts.postValue(currentPosts)

                    val entities = postsDto.map { it.toEntity() }
                    postDao.insert(entities)

                    oldestPostId = newPosts.last().id
                    hasMore = newPosts.size == count

                    Log.d(TAG, "Loaded ${newPosts.size} more posts")
                } else {
                    hasMore = false
                }

                Result.success(newPosts)
            } else {
                Result.failure(Exception("Ошибка загрузки"))
            }
        } catch (e: Exception) {
            isLoadingMore = false
            Log.e(TAG, "Exception in getPostsBefore", e)
            Result.failure(e)
        }
    }

    override suspend fun getPostsAfter(id: Long, count: Int): Result<List<Post>> {
        return try {
            val response = apiService.getPostsAfter(id, count)
            if (response.isSuccessful) {
                val postsDto = response.body() ?: emptyList()
                val newPosts = postsDto.map { it.toDomain() }

                if (newPosts.isNotEmpty()) {
                    currentPosts = newPosts + currentPosts
                    _allPosts.postValue(currentPosts)

                    val entities = postsDto.map { it.toEntity() }
                    postDao.insert(entities)
                }

                Result.success(newPosts)
            } else {
                Result.failure(Exception("Ошибка загрузки"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getPostsAfter", e)
            Result.failure(e)
        }
    }

    override suspend fun getPostById(id: Long): Result<Post> {
        return try {
            currentPosts.find { it.id == id }?.let {
                return Result.success(it)
            }
            val response = apiService.getPostById(id)
            if (response.isSuccessful) {
                val postDto = response.body() ?: return Result.failure(Exception("Пустой ответ"))
                Result.success(postDto.toDomain())
            } else {
                Result.failure(Exception("Пост не найден"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likePost(id: Long): Result<Post> {
        return try {
            val response = apiService.likePost(id)
            if (response.isSuccessful) {
                val postDto = response.body() ?: return Result.failure(Exception("Пустой ответ"))
                val updatedPost = postDto.toDomain()
                currentPosts = currentPosts.map { if (it.id == id) updatedPost else it }
                _allPosts.postValue(currentPosts)
                Result.success(updatedPost)
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
    }

    override suspend fun unlikePost(id: Long): Result<Post> {
        return try {
            val response = apiService.unlikePost(id)
            if (response.isSuccessful) {
                val postDto = response.body() ?: return Result.failure(Exception("Пустой ответ"))
                val updatedPost = postDto.toDomain()
                currentPosts = currentPosts.map { if (it.id == id) updatedPost else it }
                _allPosts.postValue(currentPosts)
                Result.success(updatedPost)
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
    }

    override suspend fun deletePost(id: Long): Result<Unit> {
        return try {
            val response = apiService.deletePost(id)
            if (response.isSuccessful) {
                currentPosts = currentPosts.filter { it.id != id }
                _allPosts.postValue(currentPosts)
                postDao.deleteById(id)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка удаления"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun savePost(
        content: String,
        attachment: Attachment?,
        coords: Coordinates?,
        mentionIds: Set<Long>?
    ): Result<Post> {
        return try {
            // Загружаем медиа, если есть
            val (mediaUrl, attachmentType) = uploadMediaIfNeeded(attachment)

            val attachmentDto = mediaUrl?.let { url ->
                AttachmentDto(url, attachmentType?.name ?: return Result.failure(Exception("No attachment type")))
            }

            val coordinatesDto = coords?.let {
                CoordinatesDto(lat = it.lat, lng = it.lng)
            }

            val request = CreatePostRequest(
                id = 0,  // id = 0 для нового поста
                content = content,
                attachment = attachmentDto,
                coords = coordinatesDto,
                mentionIds = mentionIds?.toList() ?: emptyList()
            )

            val response = apiService.savePost(request)  // Используем savePost, а не createPost

            if (response.isSuccessful) {
                val postDto = response.body() ?: return Result.failure(Exception("Empty response"))
                val newPost = postDto.toDomain()
                currentPosts = listOf(newPost) + currentPosts
                _allPosts.postValue(currentPosts)
                postDao.insert(listOf(postDto.toEntity()))
                Result.success(newPost)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving post", e)
            Result.failure(e)
        }
    }

    override suspend fun updatePost(
        id: Long,
        content: String,
        attachment: Attachment?,
        coords: Coordinates?,
        mentionIds: Set<Long>?
    ): Result<Post> {
        return try {
            // Загружаем медиа, если есть
            val (mediaUrl, attachmentType) = uploadMediaIfNeeded(attachment)

            val attachmentDto = mediaUrl?.let { url ->
                AttachmentDto(url, attachmentType?.name ?: return Result.failure(Exception("No attachment type")))
            }

            val coordinatesDto = coords?.let {
                CoordinatesDto(lat = it.lat, lng = it.lng)
            }

            val request = CreatePostRequest(
                id = id,  // Важно: передаем существующий ID!
                content = content,
                attachment = attachmentDto,
                coords = coordinatesDto,
                mentionIds = mentionIds?.toList() ?: emptyList()
            )

            val response = apiService.savePost(request)  // Используем тот же метод savePost

            if (response.isSuccessful) {
                val postDto = response.body() ?: return Result.failure(Exception("Empty response"))
                val updatedPost = postDto.toDomain()
                currentPosts = currentPosts.map { if (it.id == id) updatedPost else it }
                _allPosts.postValue(currentPosts)
                postDao.insert(listOf(postDto.toEntity()))
                Result.success(updatedPost)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating post", e)
            Result.failure(e)
        }
    }

    // Вспомогательный метод для загрузки медиа
    private suspend fun uploadMediaIfNeeded(attachment: Attachment?): Pair<String?, AttachmentType?> {
        if (attachment == null) return Pair(null, null)

        val file = File(attachment.url)
        if (!file.exists()) {
            throw Exception("File not found")
        }

        val mimeType = attachment.type.toMimeType()
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val uploadResponse = apiService.uploadMedia(part)
        if (!uploadResponse.isSuccessful) {
            throw Exception("Media upload failed")
        }

        val mediaResponse = uploadResponse.body()
        val mediaUrl = mediaResponse?.url ?: throw Exception("No URL in media response")

        return Pair(mediaUrl, attachment.type)
    }

    private fun AttachmentType.toMimeType(): String = when (this) {
        AttachmentType.IMAGE -> "image/*"
        AttachmentType.VIDEO -> "video/*"
        AttachmentType.AUDIO -> "audio/*"
    }
}