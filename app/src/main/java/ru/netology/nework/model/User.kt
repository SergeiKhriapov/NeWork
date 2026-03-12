package ru.netology.nework.model

data class User(
    val id: Long,
    val login: String,
    val name: String,
    val avatar: String?
)