package org.example.router

import com.mad.client.LoggerClient
import com.mad.model.LogLevel
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
import org.example.ILogger

/**
 * REST роутер для работы с уведомлениями
 *
 * Эндпоинты:
 * - GET /notebook/notifications/{id} - Получить уведомление по ID
 * - GET /notebook/notifications - Получить список уведомлений с постраничной навигацией
 * - POST /notebook/notifications - Создать новое уведомление
 * - POST /notebook/notifications/{id}/action - Выполнить действие над уведомлением
 */
fun Application.registerNotificationRoutes(notificationService: INotificationService, logger: ILogger) {
  routing {
    route("/notebook/notifications") {
      /**
       * Создание нового уведомления
       *
       * Тело запроса - JSON заметки (согласно модели {@Notification} без id) Возвращается созданная
       * заметка с id и датой создания
       */
      post {
        try {
          val notification = call.receive<Notification>()
          logger.logActivity(
            event = "Create notification request",
            userId = notification.userId,
            additionalData = mapOf("title" to notification.title)
          )
          
          val createNotification = notificationService.createNotification(notification)
          
          logger.logActivity(
            event = "Notification created",
            userId = notification.userId,
            additionalData = mapOf(
              "notificationId" to createNotification.id,
              "title" to createNotification.title
            )
          )
          
          call.respond(createNotification)
        } catch (e: Exception) {
          logger.logError(
            event = "Failed to create notification",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString()
          )
          call.respondText("Error creating notification: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
      }

      /**
       * Получить уведомление по ID
       *
       * ID уведомления указан в URL Если уведомление не найдено, кушаем 404
       */
      get("{id}") {
        val id = call.parameters["id"]
        if (id == null) {
          logger.logActivity(
            event = "Invalid notification ID in request",
            level = LogLevel.WARN
          )
          return@get call.respondText("Id is missing or invalid", status = HttpStatusCode.BadRequest)
        }
        
        logger.logActivity(
          event = "Get notification request",
          additionalData = mapOf("notificationId" to id)
        )
        
        val notification = notificationService.getNotification(id)
        if (notification == null) {
          logger.logActivity(
            event = "Notification not found",
            level = LogLevel.WARN,
            additionalData = mapOf("notificationId" to id)
          )
          call.respondText("Notification not found", status = HttpStatusCode.NotFound)
        } else {
          logger.logActivity(
            event = "Notification retrieved",
            userId = notification.userId,
            additionalData = mapOf("notificationId" to id)
          )
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
        
        logger.logActivity(
          event = "List notifications request",
          userId = userId,
          additionalData = mapOf(
            "page" to page.toString(),
            "pageSize" to pageSize.toString()
          )
        )
        
        val notifications = notificationService.listNotifications(userId, page, pageSize)
        
        logger.logActivity(
          event = "Notifications list retrieved",
          userId = userId,
          additionalData = mapOf(
            "count" to notifications.size.toString(),
            "page" to page.toString()
          )
        )
        
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
        val id = call.parameters["id"]
        if (id == null) {
          logger.logActivity(
            event = "Invalid notification ID in action request",
            level = LogLevel.WARN
          )
          return@post call.respondText("Id is missing or invalid", status = HttpStatusCode.BadRequest)
        }
        
        try {
          val actionRequest = call.receive<NotificationActionRequest>()
          
          logger.logActivity(
            event = "Notification action request",
            userId = actionRequest.userId,
            additionalData = mapOf(
              "notificationId" to id,
              "action" to actionRequest.action.toString(),
              "snoozeDuration" to (actionRequest.snoozeDuration?.toString() ?: "none")
            )
          )

          val updated = notificationService.performAction(
            id = id,
            userId = actionRequest.userId,
            action = actionRequest.action,
            snoozeDuration = actionRequest.snoozeDuration
          )

          if (updated == null) {
            logger.logActivity(
              event = "Notification action failed - not found",
              userId = actionRequest.userId,
              level = LogLevel.WARN,
              additionalData = mapOf(
                "notificationId" to id,
                "action" to actionRequest.action.toString()
              )
            )
            call.respondText("Notification not found or not updated", status = HttpStatusCode.NotFound)
          } else {
            logger.logActivity(
              event = "Notification action performed",
              userId = actionRequest.userId,
              additionalData = mapOf(
                "notificationId" to id,
                "action" to actionRequest.action.toString(),
                "success" to "true"
              )
            )
            call.respond(updated)
          }
        } catch (e: Exception) {
          logger.logError(
            event = "Error performing notification action - notificationId: $id",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString()
          )
          call.respondText("Error processing action: ${e.message}", status = HttpStatusCode.InternalServerError)
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
