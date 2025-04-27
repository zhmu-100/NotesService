package org.example

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import org.example.actions.NoteAction
import org.example.actions.NotificationAction
import org.example.router.registerNoteRoutes
import org.example.router.registerNotificationRoutes
import org.example.service.NoteService
import org.example.service.NotificationService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

fun Application.testModule() {
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

class AppTest {

  @Test
  fun `root path returns 404 Not Found`() {
    withTestApplication({ testModule() }) {
      handleRequest(HttpMethod.Get, "/").apply {
        assertEquals(HttpStatusCode.NotFound, response.status())
      }
    }
  }

  @Test
  fun `JSON ContentNegotiation feature is installed`() {
    withTestApplication({ testModule() }) {
      val plugin = application.pluginOrNull(ContentNegotiation)
      assertNotNull(plugin, "ContentNegotiation plugin should be installed")
    }
  }
}
