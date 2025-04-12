package org.example.model

/**
 * Enum класс, который описывает действия, которые можно выполнить с уведомлением
 * NOTIOFICATION_ACTION_UNSPECIFIED - действие не указано COMPLETE - действие завершено DISMISS -
 * действие отклонено SNOOZE - действие отложено
 */
enum class NotificationActionEnum {
  NOTIFICATION_ACTION_UNSPECIFIED,
  COMPLETE,
  DISMISS,
  SNOOZE
}
