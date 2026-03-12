package ru.netology.nework.domain.repository

import androidx.lifecycle.LiveData
import ru.netology.nework.model.Coordinates
import ru.netology.nework.model.Attachment  // Изменён импорт с MediaAttachment на Attachment
import ru.netology.nework.model.Post

interface PostRepository {
    suspend fun getPosts(): Result<List<Post>>
    suspend fun likePost(id: Long): Result<Post>
    suspend fun unlikePost(id: Long): Result<Post>

    suspend fun savePost(
        content: String?,
        attachment: Attachment?,
        coords: Coordinates?,
        mentionIds: Set<Long>?
    ): Result<Unit>

    suspend fun deletePost(id: Long): Result<Unit>

    suspend fun updatePost(
        id: Long,
        content: String?,
        attachment: Attachment?,
        coords: Coordinates?,
        mentionIds: Set<Long>?
    ): Result<Post>

    fun getPostsLiveData(): LiveData<List<Post>>
    suspend fun getPostById(id: Long): Result<Post>
}