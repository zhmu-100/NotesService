package org.example.actions

import java.net.InetAddress
import kotlin.test.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*

class NoteActionTest {
  private lateinit var server: MockWebServer
  private lateinit var action: NoteAction

  @BeforeEach
  fun setUp() {
    server = MockWebServer().apply { start(InetAddress.getByName("127.0.0.1"), 8081) }
    action = NoteAction()
  }

  @AfterEach
  fun tearDown() {
    server.shutdown()
  }
}
