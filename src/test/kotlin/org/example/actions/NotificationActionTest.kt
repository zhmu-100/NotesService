package org.example.actions

import java.net.InetAddress
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
import org.junit.jupiter.api.*

class NotificationActionTest {
  private lateinit var server: MockWebServer
  private lateinit var action: NotificationAction

  @BeforeEach
  fun setUp() {
    server = MockWebServer().apply { start(InetAddress.getByName("127.0.0.1"), 8081) }
    action = NotificationAction()
  }

  @AfterEach
  fun tearDown() {
    server.shutdown()
  }
}
