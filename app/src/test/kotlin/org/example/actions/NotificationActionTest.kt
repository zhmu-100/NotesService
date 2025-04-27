package org.example.actions

import io.ktor.server.config.MapApplicationConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.example.dto.DbCreateRequest
import org.example.dto.DbNotificationRow
import org.example.dto.DbResponse
import org.example.model.Notification
import org.example.model.NotificationActionEnum
import org.example.model.NotificationSnoozeEnum

class NotificationActionTest {
  private lateinit var server: MockWebServer
  private lateinit var action: NotificationAction

  @BeforeTest
  fun setUp() {
    server = MockWebServer().apply { start() }
    val config =
        MapApplicationConfig(
            "ktor.database.mode" to "LOCAL",
            "ktor.database.host" to server.hostName,
            "ktor.database.port" to server.port.toString())
    action = NotificationAction(config)
  }

  @AfterTest
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `createNotification posts and returns unchanged model`() = runBlocking {
    val dbResp = DbResponse(success = true, error = null)
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(dbResp))
            .addHeader("Content-Type", "application/json"))
    val input =
        Notification(
            id = "id",
            userId = "u1",
            title = "T1",
            description = "D1",
            createDate = LocalDateTime(2000, 1, 1, 0, 0),
            notificationDate = LocalDateTime(2000, 1, 1, 1, 0))

    val result = action.createNotification(input)

    assertEquals(input, result, "Expected createNotification to return input unchanged on success")
    val req = server.takeRequest()
    assertEquals("/create", req.path)
    val sent = Json.decodeFromString<DbCreateRequest>(req.body.readUtf8())
    assertEquals("notifications", sent.table)
  }

  @Test
  fun `getNotification returns model or null`() = runBlocking {
    val row =
        DbNotificationRow(
            id = "n1",
            userid = "u1",
            title = "T",
            description = "D",
            create_date = LocalDateTime(2023, 1, 1, 0, 0).toString(),
            notification_date = LocalDateTime(2023, 1, 1, 1, 0).toString())
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(listOf(row)))
            .addHeader("Content-Type", "application/json"))
    val n = action.getNotification("n1")
    assertNotNull(n)
    assertEquals(row.id, n!!.id)
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(emptyList<DbNotificationRow>()))
            .addHeader("Content-Type", "application/json"))
    assertNull(action.getNotification("no"))
  }

  @Test
  fun `listNotifications returns sorted and paged`() = runBlocking {
    val r1 =
        DbNotificationRow(
            id = "1",
            userid = "u",
            title = "t1",
            description = "d1",
            create_date = LocalDateTime(2023, 2, 1, 0, 0).toString(),
            notification_date = LocalDateTime(2023, 2, 1, 1, 0).toString())
    val r2 =
        DbNotificationRow(
            id = "2",
            userid = "u",
            title = "t2",
            description = "d2",
            create_date = LocalDateTime(2023, 1, 1, 0, 0).toString(),
            notification_date = LocalDateTime(2023, 1, 1, 1, 0).toString())
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(listOf(r1, r2)))
            .addHeader("Content-Type", "application/json"))
    val list = action.listNotifications("u", page = 1, pageSize = 10)
    assertEquals(2, list.size)
    assertEquals("1", list[0].id)
  }

  @Test
  fun `performNotificationAction COMPLETE prefixes title`() = runBlocking {
    val row =
        DbNotificationRow(
            id = "1",
            userid = "u",
            title = "T",
            description = "D",
            create_date = LocalDateTime(2023, 1, 1, 0, 0).toString(),
            notification_date = LocalDateTime(2023, 1, 1, 1, 0).toString())
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(listOf(row)))
            .addHeader("Content-Type", "application/json"))
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(DbResponse(success = true, error = null)))
            .addHeader("Content-Type", "application/json"))
    val result =
        action.performNotificationAction(
            id = "1", userId = "u", action = NotificationActionEnum.COMPLETE, snoozeDuration = null)
    assertNotNull(result)
    assertTrue(result.title.startsWith("[DONE]"))
    server.takeRequest()
    val updateReq = server.takeRequest()
    assertEquals("/update", updateReq.path)
  }

  @Test
  fun `performNotificationAction SNOOZE adjusts date`() = runBlocking {
    val base = LocalDateTime(2023, 1, 1, 0, 0)
    val row =
        DbNotificationRow(
            id = "1",
            userid = "u",
            title = "T",
            description = "D",
            create_date = base.toString(),
            notification_date = base.toString())
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(listOf(row)))
            .addHeader("Content-Type", "application/json"))
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(DbResponse(success = true, error = null)))
            .addHeader("Content-Type", "application/json"))
    val result =
        action.performNotificationAction(
            "1",
            "u",
            NotificationActionEnum.SNOOZE,
            NotificationSnoozeEnum.NOTIFICATION_SNOOZE_5_MINUTES)
    assertNotNull(result)
    assertTrue(result.notificationDate > base)
  }

  @Test
  fun `performNotificationAction returns null on mismatch`() = runBlocking {
    val row =
        DbNotificationRow(
            id = "1",
            userid = "owner",
            title = "T",
            description = "D",
            create_date = LocalDateTime(2023, 1, 1, 0, 0).toString(),
            notification_date = LocalDateTime(2023, 1, 1, 1, 0).toString())
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(listOf(row)))
            .addHeader("Content-Type", "application/json"))
    val result =
        action.performNotificationAction("1", "other", NotificationActionEnum.DISMISS, null)
    assertNull(result)
  }
}
