package org.example.router

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.example.model.Notification
import org.example.model.NotificationActionEnum
import org.example.model.NotificationSnoozeEnum
import org.example.service.INotificationService

/**
 * REST роутер для работы с уведомлениями
 *
 * Эндпоинты:
 * - GET /notebook/notifications/{id} - Получить уведомление по ID
 * - GET /notebook/notifications - Получить список уведомлений с постраничной навигацией
 * - POST /notebook/notifications - Создать новое уведомление
 * - POST /notebook/notifications/{id}/action - Выполнить действие над уведомлением
 */
fun Application.registerNotificationRoutes(notificationService: INotificationService) {
  routing {
    route("/notebook/notifications") {
      /**
       * Создание нового уведомления
       *
       * Тело запроса - JSON заметки (согласно модели {@Notification} без id) Возвращается созданная
       * заметка с id и датой создания
       */
      post {
        val notification = call.receive<Notification>()
        val createNotification = notificationService.createNotification(notification)
        call.respond(createNotification)
      }

      /**
       * Получить уведомление по ID
       *
       * ID уведомления указан в URL Если уведомление не найдено, кушаем 404
       */
      get("{id}") {
        val id =
            call.parameters["id"]
                ?: return@get call.respondText(
                    "Id is missing or invalid", status = HttpStatusCode.BadRequest)
        val notification = notificationService.getNotification(id)
        if (notification == null) {
          call.respondText("Notification not found", status = HttpStatusCode.NotFound)
        } else {
          call.respond(notification)
        }
      }

      /**
       * Получает список уведомлений для пользователя с постраничной навигацией
       *
       * Параметры query:
       * - user_id - ID пользователя
       * - page - номер страницы (по умолчанию 1)
       * - page_size - количество уведомлений на странице (по умолчанию 10)
       */
      get {
        val userId = call.request.queryParameters["user_id"] ?: ""
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 10
        val notifications = notificationService.listNotifications(userId, page, pageSize)
        call.respond(notifications)
      }

      /**
       * Выполнить действие над уведомлением
       *
       * Принимает:
       * - userId - ID пользователя
       * - action - действие см [NotificationActionEnum]
       * - snoozeDuration - время, на которое нужно отложить уведомление (опционально, см
       * [NotificationSnoozeEnum])
       */
      post("{id}/action") {
        val id =
            call.parameters["id"]
                ?: return@post call.respondText(
                    "Id is missing or invalid", status = HttpStatusCode.BadRequest)
        val actionRequest = call.receive<NotificationActionRequest>()

        val updated =
            notificationService.performAction(
                id = id,
                userId = actionRequest.userId,
                action = actionRequest.action,
                snoozeDuration = actionRequest.snoozeDuration)

        if (updated == null) {
          call.respondText(
              "Notification not found or not updated", status = HttpStatusCode.NotFound)
        } else {
          call.respond(updated)
        }
      }
    }
  }
}

/** Запрос на выполнение действия над уведомлением Не помню, зачем он нужен, но пусть будет */
@Serializable
data class NotificationActionRequest(
    val userId: String,
    val action: NotificationActionEnum,
    val snoozeDuration: NotificationSnoozeEnum? = null
)
