package ru.netology.nework.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import ru.netology.nework.data.api.dto.*

interface ApiService {
    @POST("api/users/authentication")
    suspend fun login(
        @Query("login") login: String,
        @Query("pass") password: String
    ): Response<AuthResponse>

    @Multipart
    @POST("api/users/registration")
    suspend fun registerWithoutAvatar(
        @Part("login") login: RequestBody,
        @Part("pass") password: RequestBody,
        @Part("name") name: RequestBody
    ): Response<AuthResponse>

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
    suspend fun getUser(@Path("id") id: Long): Response<UserDto>

    @GET("api/posts")
    suspend fun getPosts(): Response<List<PostDto>>

    @POST("api/posts/{id}/likes")
    suspend fun likePost(@Path("id") id: Long): Response<PostDto>

    @DELETE("api/posts/{id}/likes")
    suspend fun unlikePost(@Path("id") id: Long): Response<PostDto>

    // Новый метод для создания поста (JSON)
    @POST("api/posts")
    suspend fun createPost(
        @Body request: CreatePostRequest
    ): Response<PostDto>

    // Загрузка медиа
    @Multipart
    @POST("api/media")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): Response<MediaResponse>

    @DELETE("api/posts/{id}")
    suspend fun deletePost(@Path("id") id: Long): Response<Unit>

    @PUT("api/posts/{id}")
    suspend fun updatePost(@Path("id") id: Long, @Body request: CreatePostRequest): Response<PostDto>

}