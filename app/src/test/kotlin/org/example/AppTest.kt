package org.example

import io.github.cdimascio.dotenv.dotenv
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
  val env = dotenv { ignoreIfMissing = true }
  env.entries().forEach { entry -> System.setProperty(entry.key, entry.value) }
  val logger = ConsoleLogger()

  val noteAction = NoteAction()
  val noteService = NoteService(noteAction)
  registerNoteRoutes(noteService, logger)

  val notificationAction = NotificationAction()
  val notificationService = NotificationService(notificationAction)
  registerNotificationRoutes(notificationService, logger)
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
