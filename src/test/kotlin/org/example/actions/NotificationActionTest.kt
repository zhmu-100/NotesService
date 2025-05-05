package org.example.actions

import java.net.InetAddress
import okhttp3.mockwebserver.MockWebServer
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
