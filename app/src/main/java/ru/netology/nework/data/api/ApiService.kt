package ru.netology.nework.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import ru.netology.nework.data.api.dto.AuthResponse
import ru.netology.nework.data.api.dto.PostDto
import ru.netology.nework.data.api.dto.UserDto

interface ApiService {
    @POST("api/users/authentication")
    suspend fun login(
        @Query("login") login: String,
        @Query("pass") password: String
    ): Response<AuthResponse>

    // Регистрация без аватара (все поля как части multipart)
    @Multipart
    @POST("api/users/registration")
    suspend fun registerWithoutAvatar(
        @Part("login") login: RequestBody,
        @Part("pass") password: RequestBody,
        @Part("name") name: RequestBody
    ): Response<AuthResponse>

    // Регистрация с аватаром
    @Multipart
    @POST("api/users/registration")
    suspend fun registerWithAvatar(
        @Part("login") login: RequestBody,
        @Part("pass") password: RequestBody,
        @Part("name") name: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<AuthResponse>

    @GET("api/users")
    suspend fun getUsers(): Response<List<UserDto>>

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<UserDto>

    @GET("api/posts")
    suspend fun getPosts(): Response<List<PostDto>>

    @POST("api/posts/{id}/likes")
    suspend fun likePost(@Path("id") id: Long): Response<PostDto>

    @DELETE("api/posts/{id}/likes")
    suspend fun unlikePost(@Path("id") id: Long): Response<PostDto>
}