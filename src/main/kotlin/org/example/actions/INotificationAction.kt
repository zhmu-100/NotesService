package org.example.actions

import org.example.model.Notification
import org.example.model.NotificationActionEnum
import org.example.model.NotificationSnoozeEnum

/**
 * Интерфейс для работы с уведомлениями
 *
 * Возможные операции:
 * - Создание уведомления
 * - Получение уведомления по id
 * - Получение списка уведомлений для заданного пользователя
 * - Выполнение действия над уведомлением
 */
interface INotificationAction {
  /**
   * Создает новое уведомление
   *
   * @param notification Уведомление для сохранения
   * @return Сохраненное уведомление
   */
  suspend fun createNotification(notification: Notification): Notification

  /**
   * Получает уведомление по его ID
   *
   * @param id ID уведомления
   * @return Уведомление или null, если оно не найдено
   */
  suspend fun getNotification(id: String): Notification?

  /**
   * Получает список всех уведомлений для пользователя с учетом пейджинга
   *
   * @param userId ID пользователя
   * @param page Номер страницы (начиная с 1)
   * @param pageSize Количество записей на странице
   * @return List уведомлений
   */
  suspend fun listNotifications(userId: String, page: Int, pageSize: Int): List<Notification>

  /**
   * Выполняет действие над уведомлением
   *
   * @param id ID уведомления
   * @param userId ID пользователя
   * @param action Действие, которое нужно выполнить [NotificationActionEnum]
   * @param snoozeDuration Время, на которое нужно отложить уведомление [NotificationSnoozeEnum]
   */
  suspend fun performNotificationAction(
      id: String,
      userId: String,
      action: NotificationActionEnum,
      snoozeDuration: NotificationSnoozeEnum? = null
  ): Notification?
}
