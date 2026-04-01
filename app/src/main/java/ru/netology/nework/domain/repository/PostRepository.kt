package ru.netology.nework.domain.repository

import androidx.lifecycle.LiveData
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.Coordinates
import ru.netology.nework.model.Post

interface PostRepository {
    suspend fun getLatestPosts(count: Int = 20): Result<List<Post>>
    suspend fun getPostsBefore(id: Long, count: Int = 20): Result<List<Post>>
    suspend fun getPostsAfter(id: Long, count: Int = 20): Result<List<Post>>
    fun getPostsLiveData(): LiveData<List<Post>>
    suspend fun getPostById(id: Long): Result<Post>
    suspend fun likePost(id: Long): Result<Post>
    suspend fun unlikePost(id: Long): Result<Post>

    // Создание нового поста (id = 0)
    suspend fun savePost(
        content: String,  // убрал nullable
        attachment: Attachment?,
        coords: Coordinates?,
        mentionIds: Set<Long>?
    ): Result<Post>  // Возвращаем созданный пост

    // Обновление существующего поста
    suspend fun updatePost(
        id: Long,
        content: String,  // убрал nullable
        attachment: Attachment?,
        coords: Coordinates?,
        mentionIds: Set<Long>?
    ): Result<Post>  // Возвращаем обновленный пост

    suspend fun deletePost(id: Long): Result<Unit>
}