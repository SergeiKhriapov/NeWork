package ru.netology.nework.data.api.dto

data class UserDto(
    val id: Long,        // меняем Int на Long
    val login: String,
    val name: String,
    val avatar: String?
)