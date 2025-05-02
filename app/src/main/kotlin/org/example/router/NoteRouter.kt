package org.example.router

import com.mad.model.LogLevel
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.ILogger
import org.example.model.Note
import org.example.service.INoteService

/**
 * REST роутер для работы с заметками
 *
 * Эндпоинты:
 * - GET /notebook/notes/{id} - Получить заметку по ID
 * - GET /notebook/notes - Получить список заметок с постраничной навигацией
 * - POST /notebook/notes - Создать новую заметку
 * - PUT /notebook/notes/{id} - Обновить заметку
 * - DELETE /notebook/notes/{id} - Удалить заметку
 */
fun Application.registerNoteRoutes(noteService: INoteService, logger: ILogger) {
  routing {
    route("/notebook/notes") {
      /**
       * Создание новой заметки
       *
       * Тело запроса - JSON заметки (согласно модели [Note] без id) Возвращается созданная заметка
       * с id и датой создания
       */
      post {
        try {
          val note = call.receive<Note>()
          logger.logActivity(
              event = "Create note request",
              userId = note.userId,
              additionalData = mapOf("title" to note.title))

          val createdNote = noteService.createNote(note)

          logger.logActivity(
              event = "Note created",
              userId = note.userId,
              additionalData = mapOf("noteId" to createdNote.id, "title" to createdNote.title))

          call.respond(createdNote)
        } catch (e: Exception) {
          logger.logError(
              event = "Failed to create note",
              errorMessage = e.message ?: "Unknown error",
              stackTrace = e.stackTraceToString())
          call.respondText(
              "Error creating note: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
      }

      /**
       * Получить заметку по ID
       *
       * ID заметки указан в URL Если заметка не найдена, кушаем 404
       */
      get("{id}") {
        val id = call.parameters["id"]
        if (id == null) {
          logger.logActivity(event = "Invalid note ID in request", level = LogLevel.WARN)
          return@get call.respondText(
              "Id is missing or invalid", status = HttpStatusCode.BadRequest)
        }

        logger.logActivity(event = "Get note request", additionalData = mapOf("noteId" to id))

        val note = noteService.getNote(id)
        if (note == null) {
          logger.logActivity(
              event = "Note not found",
              level = LogLevel.WARN,
              additionalData = mapOf("noteId" to id))
          call.respondText("Note not found", status = HttpStatusCode.NotFound)
        } else {
          logger.logActivity(
              event = "Note retrieved",
              userId = note.userId,
              additionalData = mapOf("noteId" to id))
          call.respond(note)
        }
      }

      /**
       * Получает список заметок для пользователя с постраничной навигацией
       *
       * Параметры query:
       * - user_id - ID пользователя page - номер страницы (по умолчанию 1) page_size - количество
       * заметок на странице (по умолчанию 10)
       */
      get {
        val userId = call.request.queryParameters["user_id"] ?: ""
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 10

        logger.logActivity(
            event = "List notes request",
            userId = userId,
            additionalData = mapOf("page" to page.toString(), "pageSize" to pageSize.toString()))

        val notes = noteService.listNotes(userId, page, pageSize)

        logger.logActivity(
            event = "Notes list retrieved",
            userId = userId,
            additionalData = mapOf("count" to notes.size.toString(), "page" to page.toString()))

        call.respond(notes)
      }

      /**
       * Обновление заметки
       *
       * Идентификатор заметки передается в URL, а обновленные данные – в теле запроса Если заметка
       * не найдена, кушаем 404
       */
      put("{id}") {
        val id = call.parameters["id"]
        if (id == null) {
          logger.logActivity(event = "Invalid note ID in update request", level = LogLevel.WARN)
          return@put call.respondText(
              "Id is missing or invalid", status = HttpStatusCode.BadRequest)
        }

        try {
          val note = call.receive<Note>()

          logger.logActivity(
              event = "Update note request",
              userId = note.userId,
              additionalData = mapOf("noteId" to id, "title" to note.title))

          val updatedNote = noteService.updateNote(note.copy(id = id))
          if (updatedNote == null) {
            logger.logActivity(
                event = "Note update failed - not found",
                userId = note.userId,
                level = LogLevel.WARN,
                additionalData = mapOf("noteId" to id))
            call.respondText("Note not found", status = HttpStatusCode.NotFound)
          } else {
            logger.logActivity(
                event = "Note updated",
                userId = note.userId,
                additionalData = mapOf("noteId" to id, "title" to updatedNote.title))
            call.respond(updatedNote)
          }
        } catch (e: Exception) {
          logger.logError(
              event = "Failed to update note - noteId: $id",
              errorMessage = e.message ?: "Unknown error",
              stackTrace = e.stackTraceToString())
          call.respondText(
              "Error updating note: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
      }

      /**
       * Удаление заметки
       *
       * ID заметки передается в URL, а ID пользователя – в параметрах запроса Если заметка не
       * нйдена\удаление не удалось, кушаем 404
       */
      delete("{id}") {
        val id = call.parameters["id"]
        if (id == null) {
          logger.logActivity(event = "Invalid note ID in delete request", level = LogLevel.WARN)
          return@delete call.respondText(
              "Id is missing or invalid", status = HttpStatusCode.BadRequest)
        }

        val userId = call.request.queryParameters["user_id"]
        if (userId == null) {
          logger.logActivity(
              event = "Missing user_id in delete request",
              level = LogLevel.WARN,
              additionalData = mapOf("noteId" to id))
          return@delete call.respondText("Missing user_id", status = HttpStatusCode.BadRequest)
        }

        logger.logActivity(
            event = "Delete note request", userId = userId, additionalData = mapOf("noteId" to id))

        try {
          val success = noteService.deleteNote(id, userId)
          if (success) {
            logger.logActivity(
                event = "Note deleted", userId = userId, additionalData = mapOf("noteId" to id))
            call.respondText("Note deleted", status = HttpStatusCode.OK)
          } else {
            logger.logActivity(
                event = "Note deletion failed - not found",
                userId = userId,
                level = LogLevel.WARN,
                additionalData = mapOf("noteId" to id))
            call.respondText(
                "Note not found or could not be deleted", status = HttpStatusCode.NotFound)
          }
        } catch (e: Exception) {
          logger.logError(
              event = "Error deleting note - noteId: $id",
              userId = userId,
              errorMessage = e.message ?: "Unknown error",
              stackTrace = e.stackTraceToString())
          call.respondText(
              "Error deleting note: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
      }
    }
  }
}
