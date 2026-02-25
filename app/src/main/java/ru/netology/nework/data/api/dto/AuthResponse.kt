package ru.netology.nework.data.api.dto

data class AuthResponse(
    val id: Int,
    val token: String,
    val avatar: String?
)