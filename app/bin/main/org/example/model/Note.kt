package org.example.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * Data class, который описывает заметку
 *
 * Содержимое заметки:
 * - [id]
 * - идентификатор заметки
 * - [userId]
 * - идентификатор пользователя, которому принадлежит заметка
 * - [title]
 * - заголовок заметки
 * - [content]
 * - содержимое заметки
 * - [date]
 * - дата создания\обновления заметки
 */
@Serializable
data class Note(
    val id: String = "",
    val userId: String,
    val title: String,
    val content: String,
    val date: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
)
