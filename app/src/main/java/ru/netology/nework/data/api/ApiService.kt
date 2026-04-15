package ru.netology.nework.data.api

import CreatePostRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import ru.netology.nework.api.dto.EventDto
import ru.netology.nework.data.api.dto.*
import ru.netology.nework.model.Job
import ru.netology.nework.model.Post

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

    // Создание и обновление поста
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

    // Создание и обновление события
    @POST("api/events")
    suspend fun createEvent(@Body request: CreateEventRequest): Response<EventDto>

    @POST("api/events")
    suspend fun updateEvent(@Body request: CreateEventRequest): Response<EventDto>

    // Удаление события
    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): Response<Unit>

    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<UserDto>

    // Стена пользователя (посты)
    @GET("api/{authorId}/wall")
    suspend fun getUserWall(@Path("authorId") authorId: Long): Response<List<PostDto>>

    // Работы пользователя - публичный эндпоинт, работает без токена
    @GET("api/{userId}/jobs")
    suspend fun getUserJobs(@Path("userId") userId: Long): Response<List<JobDto>>

    // Создание работы - требует авторизации
    @POST("api/my/jobs")
    suspend fun createJob(@Body request: CreateJobRequest): Response<JobDto>

    // Удаление работы - требует авторизации
    @DELETE("api/my/jobs/{id}")
    suspend fun deleteJob(@Path("id") id: Long): Response<Unit>
}