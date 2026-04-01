package ru.netology.nework.data.api

import CreatePostRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import ru.netology.nework.api.dto.EventDto
import ru.netology.nework.data.api.dto.*

interface ApiService {

    // Auth
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

    // Users
    @GET("api/users")
    suspend fun getUsers(): Response<List<UserDto>>

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: Long): Response<UserDto>

    // Posts with pagination
    @GET("api/posts/latest")
    suspend fun getLatestPosts(
        @Query("count") count: Int = 20
    ): Response<List<PostDto>>

    @GET("api/posts/{id}/before")
    suspend fun getPostsBefore(
        @Path("id") id: Long,
        @Query("count") count: Int = 20
    ): Response<List<PostDto>>

    @GET("api/posts/{id}/after")
    suspend fun getPostsAfter(
        @Path("id") id: Long,
        @Query("count") count: Int = 20
    ): Response<List<PostDto>>

    @GET("api/posts/{id}")
    suspend fun getPostById(@Path("id") id: Long): Response<PostDto>

    // Likes
    @POST("api/posts/{id}/likes")
    suspend fun likePost(@Path("id") id: Long): Response<PostDto>

    @DELETE("api/posts/{id}/likes")
    suspend fun unlikePost(@Path("id") id: Long): Response<PostDto>

    // Создание и обновление поста - ОДИНАКОВЫЙ МЕТОД POST
    // Для создания нового поста: id = 0 в теле запроса
    // Для обновления: id = существующий ID поста
    @POST("api/posts")
    suspend fun savePost(@Body request: CreatePostRequest): Response<PostDto>

    // Удаление поста
    @DELETE("api/posts/{id}")
    suspend fun deletePost(@Path("id") id: Long): Response<Unit>

    // Media
    @Multipart
    @POST("api/media")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): Response<MediaResponse>

    // Events
    @GET("api/events")
    suspend fun getEvents(): Response<List<EventDto>>

    @GET("api/events/{id}")
    suspend fun getEventById(@Path("id") id: Long): Response<EventDto>

    @POST("api/events/{id}/likes")
    suspend fun likeEvent(@Path("id") id: Long): Response<EventDto>

    @DELETE("api/events/{id}/likes")
    suspend fun unlikeEvent(@Path("id") id: Long): Response<EventDto>

    @POST("api/events/{id}/participants")
    suspend fun participateEvent(@Path("id") id: Long): Response<EventDto>

    @DELETE("api/events/{id}/participants")
    suspend fun unparticipateEvent(@Path("id") id: Long): Response<EventDto>

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): Response<Unit>
}