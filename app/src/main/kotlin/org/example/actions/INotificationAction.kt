package org.example.actions

import org.example.model.Notification
import org.example.model.NotificationActionEnum
import org.example.model.NotificationSnoozeEnum

interface INotificationAction {

  suspend fun createNotification(notification: Notification): Notification

  suspend fun getNotification(id: String): Notification?

  suspend fun listNotifications(userId: String, page: Int, pageSize: Int): List<Notification>

  suspend fun performNotificationAction(
    id: String,
    userId: String,
    action: NotificationActionEnum,
    snoozeDuration: NotificationSnoozeEnum? = null
  ): Notification?
}
