package org.example.service

import org.example.model.Notification
import org.example.model.NotificationActionEnum
import org.example.model.NotificationSnoozeEnum

/**
 * Интерфейс **бизнес** логики работы с уведомлениями.
 *
 * Доступные операции
 * - Создать уведомление
 * - Получить уведомление по ID
 * - Получить список уведомлений пользователя
 * - Выполнить действие с уведомлением
 */
interface INotificationService {

  suspend fun createNotification(notification: Notification): Notification

  suspend fun getNotification(id: String): Notification?

  suspend fun listNotifications(userId: String, page: Int, pageSize: Int): List<Notification>

  suspend fun performAction(
      id: String,
      userId: String,
      action: NotificationActionEnum,
      snoozeDuration: NotificationSnoozeEnum? = null
  ): Notification?
}
