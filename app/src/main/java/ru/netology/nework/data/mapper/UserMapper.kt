package ru.netology.nework.data.mapper

import ru.netology.nework.data.api.dto.UserDto
import ru.netology.nework.model.User

fun UserDto.toDomain(): User = User(
    id = id,
    login = login,
    name = name,
    avatar = avatar
)