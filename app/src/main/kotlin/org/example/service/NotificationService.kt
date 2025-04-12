package org.example.service

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.actions.INotificationAction
import org.example.model.Notification
import org.example.model.NotificationActionEnum
import org.example.model.NotificationSnoozeEnum
import java.util.UUID

class NotificationService(private val action: INotificationAction) : INotificationService {
  override suspend fun createNotification(notification: Notification): Notification {
    val newNotification = notification.copy(
      id = UUID.randomUUID().toString(),
      createDate = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    )
    return action.createNotification((newNotification))
  }

  override suspend fun getNotification(id: String): Notification? {
    return action.getNotification(id)
  }

  override suspend fun listNotifications(userId: String, page: Int, pageSize: Int): List<Notification> {
    return action.listNotifications(userId, page, pageSize)
  }

  override suspend fun performAction(
    id: String,
    userId: String,
    actionType: NotificationActionEnum,
    snoozeDuration: NotificationSnoozeEnum?
  ): Notification? {
    return action.performNotificationAction(id, userId, actionType, snoozeDuration)
  }
}