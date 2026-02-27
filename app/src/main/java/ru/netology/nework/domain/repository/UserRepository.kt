package ru.netology.nework.domain.repository

import ru.netology.nework.model.User

interface UserRepository {
    suspend fun getUsers(): Result<List<User>>
}