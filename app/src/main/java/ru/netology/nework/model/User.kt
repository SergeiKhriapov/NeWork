package ru.netology.nework.model

data class User(
    val id: Long,           // меняем Int на Long
    val login: String,
    val name: String,
    val avatar: String?
)