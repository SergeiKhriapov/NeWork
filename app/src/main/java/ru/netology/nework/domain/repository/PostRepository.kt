package ru.netology.nework.domain.repository

import ru.netology.nework.model.MediaAttachment
import ru.netology.nework.model.Post
import androidx.lifecycle.LiveData
import androidx.lifecycle.map

interface PostRepository {
    suspend fun getPosts(): Result<List<Post>>
    suspend fun likePost(id: Long): Result<Post>
    suspend fun unlikePost(id: Long): Result<Post>
    suspend fun savePost(text: String?, attachment: MediaAttachment?): Result<Unit>
    fun getPostsLiveData(): LiveData<List<Post>> // только объявление
    suspend fun deletePost(id: Long): Result<Unit>
    suspend fun updatePost(id: Long, content: String?, attachment: MediaAttachment?): Result<Post>
}