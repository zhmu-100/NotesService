package org.example

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.example.actions.NoteAction
import org.example.router.registerNoteRoutes
import org.example.service.NoteService

/**
 * Точка входа в приложение
 *
 * Дефолтный порт - 8001
 */
fun main() {
  embeddedServer(Netty, port = 8001) {
        install(ContentNegotiation) { json() }

        val noteAction = NoteAction(environment.config)
        val noteService = NoteService(noteAction)

        registerNoteRoutes(noteService)
      }
      .start(wait = true)
}
