package ru.netology.nework.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: Long,
    val login: String,
    val name: String,
    val avatar: String?
) : Parcelable