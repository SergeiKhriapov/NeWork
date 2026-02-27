package ru.netology.nework.data.repository

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
import android.util.Log

private const val TAG = "AuthRepositoryImpl"

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(login: String, password: String): Result<User> {
        Log.d(TAG, "login called with login=$login")
        return try {
            Log.d(TAG, "Calling apiService.login")
            val authResponse = apiService.login(login, password)
            Log.d(TAG, "apiService.login returned code: ${authResponse.code()}")

            if (!authResponse.isSuccessful) {
                val errorMsg = when (authResponse.code()) {
                    400 -> "Неправильный пароль"
                    404 -> "Юзер незарегистрирован"
                    else -> "Ошибка ${authResponse.code()}"
                }
                Log.e(TAG, "Login failed: $errorMsg")
                return Result.failure(Exception(errorMsg))
            }
            val authBody = authResponse.body() ?: return Result.failure(Exception("Пустой ответ от сервера"))
            Log.d(TAG, "authBody: id=${authBody.id}, token=${authBody.token}, avatar=${authBody.avatar}")
            tokenManager.saveToken(authBody.token)
            Log.d(TAG, "Token saved")

            // Преобразуем id в Long для вызова getUser
            val userId = authBody.id.toLong()
            Log.d(TAG, "Calling apiService.getUser for id=$userId")
            val userResponse = apiService.getUser(userId)
            Log.d(TAG, "apiService.getUser returned code: ${userResponse.code()}")

            if (!userResponse.isSuccessful) {
                Log.e(TAG, "Failed to get user data, code: ${userResponse.code()}")
                return Result.failure(Exception("Не удалось получить данные пользователя"))
            }
            val userDto = userResponse.body() ?: return Result.failure(Exception("Пустой ответ"))
            val user = userDto.toDomain()
            Log.d(TAG, "user received: $user")
            tokenManager.saveUser(user) // сохраняем пользователя в DataStore
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in login", e)
            Result.failure(e)
        }
    }

    override suspend fun register(login: String, password: String, name: String, avatarFile: File?): Result<User> {
        Log.d(TAG, "register called with login=$login, name=$name, avatarFile=$avatarFile")
        return try {
            val loginBody = login.toRequestBody("text/plain".toMediaTypeOrNull())
            val passBody = password.toRequestBody("text/plain".toMediaTypeOrNull())
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())

            val authResponse = if (avatarFile != null) {
                Log.d(TAG, "Creating file part for avatar: ${avatarFile.absolutePath}")
                val requestFile = avatarFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", avatarFile.name, requestFile)
                Log.d(TAG, "Calling apiService.registerWithAvatar")
                apiService.registerWithAvatar(loginBody, passBody, nameBody, filePart)
            } else {
                Log.d(TAG, "Calling apiService.registerWithoutAvatar")
                apiService.registerWithoutAvatar(loginBody, passBody, nameBody)
            }

            Log.d(TAG, "apiService.register returned code: ${authResponse.code()}")

            if (!authResponse.isSuccessful) {
                val errorMsg = when (authResponse.code()) {
                    403 -> "Пользователь с таким логином уже зарегистрирован"
                    415 -> "Неправильный формат фото"
                    else -> "Ошибка ${authResponse.code()}"
                }
                Log.e(TAG, "Registration failed: $errorMsg")
                return Result.failure(Exception(errorMsg))
            }

            val authBody = authResponse.body() ?: return Result.failure(Exception("Пустой ответ"))
            Log.d(TAG, "authBody: id=${authBody.id}, token=${authBody.token}, avatar=${authBody.avatar}")
            tokenManager.saveToken(authBody.token)
            Log.d(TAG, "Token saved")

            // Преобразуем id в Long
            val userId = authBody.id.toLong()
            Log.d(TAG, "Calling apiService.getUser for id=$userId")
            val userResponse = apiService.getUser(userId)
            Log.d(TAG, "apiService.getUser returned code: ${userResponse.code()}")

            if (!userResponse.isSuccessful) {
                Log.e(TAG, "Failed to get user data, code: ${userResponse.code()}")
                return Result.failure(Exception("Не удалось получить данные пользователя"))
            }

            val userDto = userResponse.body() ?: return Result.failure(Exception("Пустой ответ"))
            val user = userDto.toDomain()
            Log.d(TAG, "user received: $user")
            tokenManager.saveUser(user) // сохраняем пользователя в DataStore
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in register", e)
            Result.failure(e)
        }
    }
}