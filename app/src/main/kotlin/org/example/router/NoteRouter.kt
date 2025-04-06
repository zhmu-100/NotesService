package org.example.router

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.actions.NoteAction
import org.example.model.Note
import org.example.service.INoteService
import org.example.service.NoteService

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
fun Application.registerNoteRoutes(noteService: INoteService) {
  routing {
    route("/notebook/notes") {
      /**
       * Создание новой заметки
       *
       * Тело запроса - JSON заметки (согласно модели [Note] без id)
       * Возвращается созданная заметка с id и датой создания
       */
      post {
        val note = call.receive<Note>()
        val createdNote = noteService.createNote(note)
        call.respond(createdNote)
      }

      /**
       * Получить заметку по ID
       *
       * ID заметки указан в URL
       * Если заметка не найдена, кушаем 404
       */
      get("{id}") {
        val id = call.parameters["id"] ?: return@get call.respondText(
          "Id is missing or invalid",
          status = HttpStatusCode.BadRequest
        )
        val note = noteService.getNote(id)
        if (note == null) {
          call.respondText("Note not found", status = HttpStatusCode.NotFound)
        } else {
          call.respond(note)
        }
      }

      /**
       * Получает список заметок для пользователя с постраничной навигацией
       *
       * Параметры query:
       * - user_id - ID пользователя
       * page - номер страницы (по умолчанию 1)
       * page_size - количество заметок на странице (по умолчанию 10)
       */
      get {
        val userId = call.request.queryParameters["user_id"] ?: ""
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: 10
        val notes = noteService.listNotes(userId, page, pageSize)
        call.respond(notes)
      }

      /**
       * Обновление заметки
       *
       * Идентификатор заметки передается в URL, а обновленные данные – в теле запроса
       * Если заметка не найдена, кушаем 404
       */
      put("{id}") {
        val id = call.parameters["id"] ?: return@put call.respondText(
          "Id is missing or invalid",
          status = HttpStatusCode.BadRequest
        )
        val note = call.receive<Note>()
        val updatedNote = noteService.updateNote(note.copy(id = id))
        if (updatedNote == null) {
          call.respondText("Note not found", status = HttpStatusCode.NotFound)
        } else {
          call.respond(updatedNote)
        }
      }

      /**
       * Удаление заметки
       *
       * ID заметки передается в URL, а ID пользователя – в параметрах запроса
       * Если заметка не нйдена\удаление не удалось, кушаем 404
       */
      delete("{id}") {
        val id = call.parameters["id"] ?: return@delete call.respondText(
          "Id is missing or invalid",
          status = HttpStatusCode.BadRequest
        )
        val userId = call.request.queryParameters["user_id"] ?: return@delete call.respondText(
          "Missing user_id",
          status = HttpStatusCode.BadRequest
        )
        val success = noteService.deleteNote(id, userId)
        if (success) {
          call.respondText("Note deleted", status = HttpStatusCode.OK)
        } else {
          call.respondText("Note not found or could not be deleted", status = HttpStatusCode.NotFound)
        }
      }
    }
  }
}