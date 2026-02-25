package ru.netology.nework.data.api.dto


data class UserDto(
    val id: Int,
    val login: String,
    val name: String,
    val avatar: String?
)