package org.example.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Data class, который описывает заметку
 *
 * Содержимое заметки:
 * - [id] - идентификатор заметки
 * - [userId] - идентификатор пользователя, которому принадлежит заметка
 * - [title] - заголовок заметки
 * - [content] - содержимое заметки
 * - [date] - дата создания\обновления заметки
 */
@Serializable
data class Note(
  val id: String,
  val userId: String,
  val title: String,
  val content: String,
  val date: Instant
)
