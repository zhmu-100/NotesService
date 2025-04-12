package org.example.actions

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.example.model.Note

/**
 * Реализация интерфейса [INoteAction]. Может работать с локальной БД или через API Gateway
 *
 * @see INoteAction
 * @property config Конфигурация приложения, используется для определения адреса БД
 */
class NoteAction(private val config: ApplicationConfig) : INoteAction {

  private val dbMode = config.propertyOrNull("ktor.database.mode")?.getString() ?: "LOCAL"
  private val dbHost = config.propertyOrNull("ktor.database.host")?.getString() ?: "localhost"
  private val dbPort = config.propertyOrNull("ktor.database.port")?.getString() ?: "8080"

  private val baseUrl =
      if (dbMode == "gateway") {
        "http://$dbHost:$dbPort/api/db"
      } else {
        "http://$dbHost:$dbPort"
      }

  private val httpClient = HttpClient { install(ContentNegotiation) { json() } }

  /**
   * Запрос на создание записи в БД
   *
   * @property table Название таблицы
   * @property data Данные, которые нужно вставить
   */
  @Serializable data class DbCreateRequest(val table: String, val data: Map<String, String>)

  /**
   * Запрос на чтение данных из БД
   *
   * @property table Название таблицы
   * @property columns Список столбцов, которые необходимо прочитать, в данном случае читаем все
   * @property filters Фильтры для запроса
   */
  @Serializable
  data class DbReadRequest(
      val table: String,
      val columns: List<String> = listOf("*"),
      val filters: Map<String, String>? = null
  )

  /**
   * Запрос на обновление данных в БД.
   *
   * @property table Название таблицы
   * @property data Новые значения
   * @property condition Условие обновления
   * @property conditionParams Параметры для условия
   */
  @Serializable
  data class DbUpdateRequest(
      val table: String,
      val data: Map<String, String>,
      val condition: String,
      val conditionParams: List<String>
  )

  /**
   * Запрос на удаление записи из БД
   *
   * @property table Название таблицы
   * @property condition Условие удаления
   * @property conditionParams Параметры условия
   */
  @Serializable
  data class DbDeleteRequest(
      val table: String,
      val condition: String,
      val conditionParams: List<String>
  )

  /**
   * Ответ от БД на операции создания/обновления/удаления
   *
   * @property success Признак успешности
   * @property error Сообщение об ошибке, если есть
   */
  @Serializable data class DbResponse(val success: Boolean? = null, val error: String? = null)

  /**
   * Строка заметки из БД
   *
   * @property id Идентификатор заметки
   * @property userid Идентификатор пользователя
   * @property title Заголовок заметки
   * @property content Содержимое заметки
   * @property date Дата создания заметки
   */
  @Serializable
  data class DbNoteRow(
      val id: String,
      val userid: String,
      val title: String,
      val content: String,
      val date: String
  )

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
