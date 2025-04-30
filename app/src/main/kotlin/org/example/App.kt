package org.example

import io.github.cdimascio.dotenv.dotenv
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.example.actions.NoteAction
import org.example.actions.NotificationAction
import org.example.router.registerNoteRoutes
import org.example.router.registerNotificationRoutes
import org.example.service.NoteService
import org.example.service.NotificationService

/**
 * Точка входа в приложение
 *
 * Дефолтный порт - 8001
 */
fun main() {
  val dotenv = dotenv()
  val port = dotenv["SERVICE_PORT"]?.toIntOrNull() ?: 8001
  embeddedServer(Netty, port = port) {
        install(ContentNegotiation) { json() }

        // notes
        val noteAction = NoteAction()
        val noteService = NoteService(noteAction)
        registerNoteRoutes(noteService)

        // notifications
        val notificationAction = NotificationAction()
        val notificationService = NotificationService(notificationAction)
        registerNotificationRoutes(notificationService)
      }
      .start(wait = true)
}
