package org.example.actions

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.example.model.Notification
import org.example.model.NotificationActionEnum
import org.example.model.NotificationSnoozeEnum

/**
 * Реализация интерфейса [INotificationAction]. Может работать с локальной БД или через API Gateway
 *
 * @see INotificationAction
 * @property config Конфигурация приложения, используется для определения адреса БД
 */
class NotificationAction(private val config: ApplicationConfig) : INotificationAction {

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
   * Запрос на удаление данных в БД.
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
   * Ответ от БД
   *
   * @property success Успех операции
   * @property error Сообщение об ошибке
   */
  @Serializable data class DbResponse(val success: Boolean? = null, val error: String? = null)

  /**
   * Строка таблицы уведомлений
   *
   * @property id ID уведомления
   * @property userid ID пользователя
   * @property title Заголовок уведомления
   * @property description Описание уведомления
   * @property create_date Дата создания уведомления
   * @property notification_date Дата уведомления
   */
  @Serializable
  data class DbNotificationRow(
      val id: String,
      val userid: String,
      val title: String,
      val description: String,
      val create_date: String,
      val notification_date: String
  )

  /**
   * Создает новое уведомление
   *
   * @param notification Уведомление для сохранения
   * @return Сохраненное уведомление
   * @throws Exception Если не удалось сохранить уведомление
   */
  override suspend fun createNotification(notification: Notification): Notification =
      withContext(Dispatchers.IO) {
        val requestBody =
            DbCreateRequest(
                table = "notifications",
                data =
                    mapOf(
                        "id" to notification.id,
                        "userid" to notification.userId,
                        "title" to notification.title,
                        "description" to notification.description,
                        "create_date" to notification.createDate.toString(),
                        "notification_date" to notification.notificationDate.toString()))
        val url = "$baseUrl/create"
        val response: HttpResponse =
            httpClient.post(url) {
              contentType(ContentType.Application.Json)
              setBody(requestBody)
            }

        val dbResp: DbResponse = response.body()
        if (dbResp.success == true) {
          println("Created notification in DB: $notification")
          notification
        } else {
          throw Exception("Failed to create notification: ${dbResp.error}")
        }
      }

  /**
   * Получает уведомление по его ID
   *
   * @param id ID уведомления
   * @return Уведомление или null, если оно не найдено
   */
  override suspend fun getNotification(id: String): Notification? =
      withContext(Dispatchers.IO) {
        val requestBody =
            DbReadRequest(
                table = "notifications", columns = listOf("*"), filters = mapOf("id" to id))
        val url = "$baseUrl/read"
        val response: HttpResponse =
            httpClient.post(url) {
              contentType(ContentType.Application.Json)
              setBody(requestBody)
            }

        val rows: List<DbNotificationRow> = response.body()
        println("Retrieving notification with id=$id -> $rows")

        if (rows.isEmpty()) {
          return@withContext null
        }

        val row = rows.first()

        return@withContext row.toModel()
      }

  /**
   * Получает список уведомлений для заданного пользователя с учетом пейджинга
   *
   * @param userId ID пользователя
   * @param page Номер страницы (начиная с 1)
   * @param pageSize Количество записей на странице
   * @return Список уведомлений
   */
  override suspend fun listNotifications(
      userId: String,
      page: Int,
      pageSize: Int
  ): List<Notification> =
      withContext(Dispatchers.IO) {
        val filtersMap = if (userId.isBlank()) null else mapOf("userid" to userId)
        val requestBody =
            DbReadRequest(
                table = "notifications",
                columns =
                    listOf(
                        "id", "userid", "title", "description", "create_date", "notification_date"),
                filters = filtersMap)
        val url = "$baseUrl/read"
        val response: HttpResponse =
            httpClient.post(url) {
              contentType(ContentType.Application.Json)
              setBody(requestBody)
            }

        val rows: List<DbNotificationRow> = response.body()
        println("Listing notifications for user=$userId -> found: ${rows.size}")

        val sorted = rows.sortedByDescending { LocalDateTime.parse(it.create_date) }

        val sliced = sorted.drop((page - 1) * pageSize).take(pageSize)

        return@withContext sliced.map { it.toModel() }
      }

  /**
   * Выполняет действие над уведомлением
   *
   * @param id ID уведомления
   * @param userId ID пользователя
   * @param action Действие, которое нужно выполнить [NotificationActionEnum]
   * @param snoozeDuration Время, на которое нужно отложить уведомление [NotificationSnoozeEnum]
   * @return Уведомление или null, если оно не найдено
   * @throws Exception Если не удалось выполнить действие
   */
  override suspend fun performNotificationAction(
      id: String,
      userId: String,
      action: NotificationActionEnum,
      snoozeDuration: NotificationSnoozeEnum?
  ): Notification? =
      withContext(Dispatchers.IO) {
        val existing = getNotification(id) ?: return@withContext null

        if (existing.userId != userId) {
          println("Notification $id belongs to ${existing.userId}, not $userId!")
          return@withContext null
        }

        val updated =
            when (action) {
              NotificationActionEnum.COMPLETE -> existing.copy(title = "[DONE] ${existing.title}")
              NotificationActionEnum.DISMISS ->
                  existing.copy(title = "[DISMISSED] ${existing.title}")
              NotificationActionEnum.SNOOZE -> {
                val oldDate = existing.notificationDate
                val newDate = oldDate.plusSnooze(snoozeDuration)
                existing.copy(notificationDate = newDate)
              }
              NotificationActionEnum.NOTIFICATION_ACTION_UNSPECIFIED -> existing
            }

        val requestBody =
            DbUpdateRequest(
                table = "notifications",
                data =
                    mapOf(
                        "userid" to updated.userId,
                        "title" to updated.title,
                        "description" to updated.description,
                        "create_date" to updated.createDate.toString(),
                        "notification_date" to updated.notificationDate.toString()),
                condition = "id = ?",
                conditionParams = listOf(updated.id))
        val url = "$baseUrl/update"
        val response: HttpResponse =
            httpClient.put(url) {
              contentType(ContentType.Application.Json)
              setBody(requestBody)
            }
        val dbResp: DbResponse = response.body()

        return@withContext if (dbResp.success == true) {
          println("performNotificationAction($action) done. Updated: $updated")
          updated
        } else {
          println("Failed to update notification $id: ${dbResp.error}")
          null
        }
      }

  /**
   * Преобразует строку таблицы уведомлений в модель уведомления
   *
   * @receiver DbNotificationRow Строка таблицы уведомлений
   * @return Notification Модель уведомления
   */
  private fun DbNotificationRow.toModel(): Notification {
    return Notification(
        id = this.id,
        userId = this.userid,
        title = this.title,
        description = this.description,
        createDate = LocalDateTime.parse(this.create_date),
        notificationDate = LocalDateTime.parse(this.notification_date))
  }

  /**
   * Добавляет время задержки к дате уведомления
   *
   * @receiver LocalDateTime Дата уведомления
   * @param snooze Время задержки
   * @return LocalDateTime Новая дата уведомления
   */
  private fun LocalDateTime.plusSnooze(snooze: NotificationSnoozeEnum?): LocalDateTime {
    if (snooze == null) return this

    val duration: Duration =
        when (snooze) {
          NotificationSnoozeEnum.NOTIFICATION_SNOOZE_UNSPECIFIED -> Duration.ZERO
          NotificationSnoozeEnum.NOTIFICATION_SNOOZE_5_MINUTES -> 5.minutes
          NotificationSnoozeEnum.NOTIFICATION_SNOOZE_15_MINUTES -> 15.minutes
          NotificationSnoozeEnum.NOTIFICATION_SNOOZE_30_MINUTES -> 30.minutes
          NotificationSnoozeEnum.NOTIFICATION_SNOOZE_1_HOUR -> 1.hours
          NotificationSnoozeEnum.NOTIFICATION_SNOOZE_5_HOURS -> 5.hours
          NotificationSnoozeEnum.NOTIFICATION_SNOOZE_1_DAY -> 1.days
        }

    val instant = this.toInstant(TimeZone.UTC)
    val newInstant = instant.plus(duration)
    return newInstant.toLocalDateTime(TimeZone.UTC)
  }
}
