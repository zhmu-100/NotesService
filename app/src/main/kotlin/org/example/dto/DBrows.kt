package org.example.dto

import kotlinx.serialization.Serializable

/**
 * Строка заметки из БД
 *
 * @property id Идентификатор заметки
 * @property userid Идентификатор пользователя
 * @property title Заголовок заметки
 * @property content Содержимое заметки
 * @property date Дата создания заметки
 */
@Serializable
data class DbNoteRow(
    val id: String,
    val userid: String,
    val title: String,
    val content: String,
    val date: String
)

/**
 * Строка таблицы уведомлений
 *
 * @property id ID уведомления
 * @property userid ID пользователя
 * @property title Заголовок уведомления
 * @property description Описание уведомления
 * @property create_date Дата создания уведомления
 * @property notification_date Дата уведомления
 */
@Serializable
data class DbNotificationRow(
    val id: String,
    val userid: String,
    val title: String,
    val description: String,
    val create_date: String,
    val notification_date: String
)
