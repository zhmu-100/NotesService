package org.example.service

import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.actions.INotificationAction
import org.example.model.Notification
import org.example.model.NotificationActionEnum
import org.example.model.NotificationSnoozeEnum

/**
 * Реализация интерфейса [INotificationService] для бизнес логики работы с уведомлениями.
 * Существование данного класса под вопросом, но наверное хорошо разделять обращение к бд и бизнес
 * логику. Философии не будет
 */
class NotificationService(private val action: INotificationAction) : INotificationService {

  /**
   * Создает новое уведомление. Генерит UUID и дату создания.
   *
   * @param notification Уведомление для создания
   * @return Созданное уведомление с UUID и датой создания
   */
  override suspend fun createNotification(notification: Notification): Notification {
    val newNotification =
        notification.copy(
            id = UUID.randomUUID().toString(),
            createDate = Clock.System.now().toLocalDateTime(TimeZone.UTC))
    return action.createNotification((newNotification))
  }

  /**
   * Получает уведомление по его ID
   *
   * @param id ID уведомления
   * @return Уведомление или null, если оно не найдено
   */
  override suspend fun getNotification(id: String): Notification? {
    return action.getNotification(id)
  }

  /**
   * Получает List<Notification> уведомлений для пользователя с постраничной навигацией
   *
   * @param userId ID пользователя
   * @param page Номер страницы (начиная с 1)
   * @param pageSize Количество напоминаний на странице
   * @return Список уведомлений
   */
  override suspend fun listNotifications(
      userId: String,
      page: Int,
      pageSize: Int
  ): List<Notification> {
    return action.listNotifications(userId, page, pageSize)
  }

  /**
   * Обновляет существующее уведомление. Обновляет дату обновления.
   *
   * @param id ID напоминания
   * @param userId ID пользователя
   * @param actionType Тип действия, см [NotificationActionEnum]
   * @param snoozeDuration Время отложки, см [NotificationSnoozeEnum]
   * @return Обновленное уведомление
   */
  override suspend fun performAction(
      id: String,
      userId: String,
      actionType: NotificationActionEnum,
      snoozeDuration: NotificationSnoozeEnum?
  ): Notification? {
    return action.performNotificationAction(id, userId, actionType, snoozeDuration)
  }
}
