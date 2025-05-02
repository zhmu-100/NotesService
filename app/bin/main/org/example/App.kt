package org.example

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
  embeddedServer(Netty, port = 8001) {
        install(ContentNegotiation) { json() }

        // notes
        val noteAction = NoteAction(environment.config)
        val noteService = NoteService(noteAction)
        registerNoteRoutes(noteService)

        // notifications
        val notificationAction = NotificationAction(environment.config)
        val notificationService = NotificationService(notificationAction)
        registerNotificationRoutes(notificationService)
      }
      .start(wait = true)
}
