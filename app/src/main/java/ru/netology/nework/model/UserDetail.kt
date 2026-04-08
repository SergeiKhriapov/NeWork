// UserDetail.kt - добавить, если нет
package ru.netology.nework.model

data class UserDetail(
    val user: User,
    val wallPosts: List<Post>,
    val jobs: List<Job>
)