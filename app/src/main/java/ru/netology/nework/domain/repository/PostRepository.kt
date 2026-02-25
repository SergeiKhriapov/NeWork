package ru.netology.nework.domain.repository

import ru.netology.nework.model.Post

interface PostRepository {
    suspend fun getPosts(): Result<List<Post>>
}