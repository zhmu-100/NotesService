package org.example.actions

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import org.example.dto.DbCreateRequest
import org.example.dto.DbDeleteRequest
import org.example.dto.DbNoteRow
import org.example.dto.DbReadRequest
import org.example.dto.DbResponse
import org.example.dto.DbUpdateRequest
import org.example.model.Note

/**
 * Реализация интерфейса [INoteAction]. Может работать с локальной БД или через API Gateway
 *
 * @see INoteAction
 */
class NoteAction : INoteAction {

  private val dotenv = dotenv { ignoreIfMissing = true }
  private val dbMode = dotenv["DB_MODE"] ?: "LOCAL"
  private val dbHost = dotenv["DB_HOST"] ?: "localhost"
  private val dbPort = dotenv["DB_PORT"] ?: "8080"
  private val baseUrl =
      if (dbMode.equals("gateway", true)) {
        "http://$dbHost:$dbPort/api/db"
      } else {
        "http://$dbHost:$dbPort"
      }

  private val httpClient = HttpClient { install(ContentNegotiation) { json() } }

  /**
   * Создает новую заметку
   *
   * @param note Заметка для создания
   * @return Созданная заметка
   * @throws Exception если создание не удалось
   */
  override suspend fun createNote(note: Note): Note =
      withContext(Dispatchers.IO) {
        val requestBody =
            DbCreateRequest(
                table = "notes",
                data =
                    mapOf(
                        "id" to note.id,
                        "userId" to note.userId,
                        "title" to note.title,
                        "content" to note.content,
                        "date" to note.date.toString()))
        println(requestBody)
        val url = "$baseUrl/create"
        val response: HttpResponse =
            httpClient.post(url) {
              contentType(ContentType.Application.Json)
              setBody(requestBody)
            }

        val dbResp: DbResponse = response.body()
        if (dbResp.success == true) {
          println("Created note in db: $note")
          note
        } else {
          throw Exception("Failed to create note: ${dbResp.error}")
        }
      }

  /**
   * Получает заметку по идентификатору
   *
   * @param id Идентификатор заметки
   * @return Найденная заметка или null, если не найдена
   */
  override suspend fun getNote(id: String): Note? =
      withContext(Dispatchers.IO) {
        val requestBody =
            DbReadRequest(table = "notes", columns = listOf("*"), filters = mapOf("id" to id))
        val url = "$baseUrl/read"
        val response: HttpResponse =
            httpClient.post(url) {
              contentType(ContentType.Application.Json)
              setBody(requestBody)
            }

        val rows: List<DbNoteRow> = response.body()
        println("Retrieving note with id=$id -> $rows")

        val firstRow = rows.firstOrNull() ?: return@withContext null

        val localDateTime = LocalDateTime.parse(firstRow.date)
        return@withContext Note(
            id = firstRow.id,
            userId = firstRow.userid,
            title = firstRow.title,
            content = firstRow.content,
            date = localDateTime)
      }

  /**
   * Получает список заметок пользователя с пагинацией (умное слово)
   *
   * @param userId Идентификатор пользователя
   * @param page Номер страницы
   * @param pageSize Количество заметок на странице
   * @return Список заметок
   */
  override suspend fun listNotes(userId: String, page: Int, pageSize: Int): List<Note> =
      withContext(Dispatchers.IO) {
        val filtersMap = if (userId.isEmpty()) null else mapOf("userid" to userId)

        val requestBody =
            DbReadRequest(
                table = "notes",
                columns = listOf("id", "userid", "title", "content", "date"),
                filters = filtersMap)

        val url = "$baseUrl/read"
        val response: HttpResponse =
            httpClient.post(url) {
              contentType(ContentType.Application.Json)
              setBody(requestBody)
            }

        val rows: List<DbNoteRow> = response.body()
        println("Listing notes for user: $userId -> found: ${rows.size}")

        val sorted = rows.sortedByDescending { LocalDateTime.parse(it.date) }

        val sliced = sorted.drop((page - 1) * pageSize).take(pageSize)

        return@withContext sliced.map { row ->
          Note(
              id = row.id,
              userId = row.userid,
              title = row.title,
              content = row.content,
              date = LocalDateTime.parse(row.date))
        }
      }

  /**
   * Обновляет существующую заметку
   *
   * @param note Заметка с новыми данными
   * @return Обновленная заметка или null, если не найдена или ошибка
   */
  override suspend fun updateNote(note: Note): Note? =
      withContext(Dispatchers.IO) {
        val requestBody =
            DbUpdateRequest(
                table = "notes",
                data =
                    mapOf(
                        "userId" to note.userId,
                        "title" to note.title,
                        "content" to note.content,
                        "date" to note.date.toString()),
                condition = "id = ?",
                conditionParams = listOf(note.id))

        val url = "$baseUrl/update"
        val response: HttpResponse =
            httpClient.put(url) {
              contentType(ContentType.Application.Json)
              setBody(requestBody)
            }

        val dbResp: DbResponse = response.body()
        return@withContext if (dbResp.success == true) {
          println("Updating note: $note")
          note
        } else {
          println("Failed to update note with id=${note.id}: ${dbResp.error}")
          null
        }
      }

  /**
   * Удаляет заметку, если она принадлежит указанному пользователю
   *
   * @param id Идентификатор заметки
   * @param userId Идентификатор пользователя
   * @return true, если удаление прошло успешно, иначе false
   */
  override suspend fun deleteNote(id: String, userId: String): Boolean =
      withContext(Dispatchers.IO) {
        val existing = getNote(id) ?: return@withContext false
        if (existing.userId != userId) {
          println("Deleting note $id for user: $userId - but it belongs to ${existing.userId}")
          return@withContext false
        }

        val requestBody =
            DbDeleteRequest(table = "notes", condition = "id = ?", conditionParams = listOf(id))

        val url = "$baseUrl/delete"
        val response: HttpResponse =
            httpClient.delete(url) {
              contentType(ContentType.Application.Json)
              setBody(requestBody)
            }

        val dbResp: DbResponse = response.body()
        val success = (dbResp.success == true)
        println("Deleting note $id for user: $userId -> success=$success")
        return@withContext success
      }
}
