package ru.netology.nework.domain.repository

import ru.netology.nework.model.Post

interface PostRepository {
    suspend fun getPosts(): Result<List<Post>>
    suspend fun likePost(id: Long): Result<Post>
    suspend fun unlikePost(id: Long): Result<Post>
}