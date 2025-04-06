package org.example


import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.example.router.registerNoteRoutes

/**
 * Точка входа в приложение
 *
 * Дефолтный порт - 8001
 */
fun main() {
  embeddedServer(Netty, port = 8001) {
    install(ContentNegotiation) { json() }
    registerNoteRoutes()
  }.start(wait = true)
}
