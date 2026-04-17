package ru.netology.nework.data.repository

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.data.api.ApiService
import ru.netology.nework.data.datastore.TokenManager
import ru.netology.nework.data.mapper.toDomain
import ru.netology.nework.domain.repository.AuthRepository
import ru.netology.nework.model.User
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(login: String, password: String): Result<User> {
        return try {
            val authResponse = apiService.login(login, password)

            if (!authResponse.isSuccessful) {
                val errorMsg = when (authResponse.code()) {
                    400 -> "Incorrect password"
                    404 -> "User not registered"
                    else -> "Error ${authResponse.code()}"
                }
                return Result.failure(Exception(errorMsg))
            }
            val authBody = authResponse.body() ?: return Result.failure(Exception("Empty server response"))
            tokenManager.saveToken(authBody.token)

            val userId = authBody.id.toLong()
            val userResponse = apiService.getUser(userId)

            if (!userResponse.isSuccessful) {
                return Result.failure(Exception("Failed to get user data"))
            }
            val userDto = userResponse.body() ?: return Result.failure(Exception("Empty response"))
            val user = userDto.toDomain()
            tokenManager.saveUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(login: String, password: String, name: String, avatarFile: File?): Result<User> {
        return try {
            val loginBody = login.toRequestBody("text/plain".toMediaTypeOrNull())
            val passBody = password.toRequestBody("text/plain".toMediaTypeOrNull())
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())

            val authResponse = if (avatarFile != null) {
                val requestFile = avatarFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", avatarFile.name, requestFile)
                apiService.registerWithAvatar(loginBody, passBody, nameBody, filePart)
            } else {
                apiService.registerWithoutAvatar(loginBody, passBody, nameBody)
            }

            if (!authResponse.isSuccessful) {
                val errorMsg = when (authResponse.code()) {
                    403 -> "User with this login already exists"
                    415 -> "Invalid photo format"
                    else -> "Error ${authResponse.code()}"
                }
                return Result.failure(Exception(errorMsg))
            }

            val authBody = authResponse.body() ?: return Result.failure(Exception("Empty response"))
            tokenManager.saveToken(authBody.token)

            val userId = authBody.id.toLong()
            val userResponse = apiService.getUser(userId)

            if (!userResponse.isSuccessful) {
                return Result.failure(Exception("Failed to get user data"))
            }

            val userDto = userResponse.body() ?: return Result.failure(Exception("Empty response"))
            val user = userDto.toDomain()
            tokenManager.saveUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}