package org.example.actions

import java.net.InetAddress
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.example.dto.DbCreateRequest
import org.example.dto.DbNoteRow
import org.example.dto.DbResponse
import org.example.model.Note
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test

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
