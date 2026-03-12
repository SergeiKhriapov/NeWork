package ru.netology.nework.utils

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun OffsetDateTime.formatForDisplay(): String = this.format(displayFormatter)
}