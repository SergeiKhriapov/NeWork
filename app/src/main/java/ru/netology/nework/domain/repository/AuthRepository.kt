package ru.netology.nework.domain.repository

import ru.netology.nework.model.User
import java.io.File

interface AuthRepository {
    suspend fun login(login: String, password: String): Result<User>
    suspend fun register(login: String, password: String, name: String, avatarFile: File?): Result<User>
}