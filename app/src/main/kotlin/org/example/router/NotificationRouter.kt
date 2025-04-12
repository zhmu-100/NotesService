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

fun Application.registerNotificationRoutes(notificationService: INotificationService) {
  routing {
    route("/notebook/notifications") {
      post {
        val notification = call.receive<Notification>()
        val createNotification = notificationService.createNotification(notification)
        call.respond(createNotification)
      }

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

      get {
        val userId = call.request.queryParameters["user_id"] ?: ""
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 10
        val notifications = notificationService.listNotifications(userId, page, pageSize)
        call.respond(notifications)
      }

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

@Serializable
data class NotificationActionRequest(
    val userId: String,
    val action: NotificationActionEnum,
    val snoozeDuration: NotificationSnoozeEnum? = null
)
