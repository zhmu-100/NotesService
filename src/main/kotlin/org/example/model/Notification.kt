package org.example.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Data class, который описывает уведомление
 *
 * Содержимое уведомления:
 * - [id] идентификатор уведомления
 * - [userId] идентификатор пользователя, которому принадлежит уведомление
 * - [title] заголовок уведомления
 * - [description] содержимое уведомления
 * - [createDate] дата создания уведомления
 * - [notificationDate] дата уведомления
 */
@Serializable
data class Notification(
    val id: String = "",
    val userId: String,
    val title: String,
    val description: String,
    val createDate: LocalDateTime,
    val notificationDate: LocalDateTime
)
