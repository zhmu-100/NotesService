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
    server = MockWebServer().apply { start(InetAddress.getByName("127.0.0.1"), 8082) }
    action = NoteAction()
  }

  @AfterEach
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `createNote posts to create endpoint and returns input on success`() = runBlocking {
    val dbResp = DbResponse(success = true, error = null)
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(dbResp))
            .addHeader("Content-Type", "application/json"))

    val now = LocalDateTime(2000, 1, 1, 0, 0)
    val input = Note(id = "id0", userId = "user1", title = "T", content = "C", date = now)
    val result = action.createNote(input)

    assertTrue(result.id.isNotBlank())
    assertTrue(result.date >= now)
    assertEquals(input.userId, result.userId)
    assertEquals(input.title, result.title)
    assertEquals(input.content, result.content)

    val request = server.takeRequest()
    assertEquals("/create", request.path)
    val sent = Json.decodeFromString<DbCreateRequest>(request.body.readUtf8())
    assertEquals("notes", sent.table)
    assertEquals(input.id, sent.data["id"])
  }

  @Test
  fun `getNote returns parsed Note or null when empty`() = runBlocking {
    val row =
        DbNoteRow(
            id = "1",
            userid = "u1",
            title = "t",
            content = "c",
            date = LocalDateTime(2023, 1, 1, 1, 1).toString())
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(listOf(row)))
            .addHeader("Content-Type", "application/json"))

    val note = action.getNote("1")

    assertNotNull(note)
    assertEquals(row.id, note!!.id)
    assertEquals(row.userid, note.userId)
    assertEquals(row.title, note.title)
    assertEquals(row.content, note.content)
    assertEquals(LocalDateTime.parse(row.date), note.date)

    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(emptyList<DbNoteRow>()))
            .addHeader("Content-Type", "application/json"))
    val note2 = action.getNote("x")
    assertNull(note2)
  }

  @Test
  fun `listNotes sends filters and returns sorted results`() = runBlocking {
    val r1 =
        DbNoteRow(
            id = "1",
            userid = "u",
            title = "t1",
            content = "c1",
            date = LocalDateTime(2023, 2, 1, 0, 0).toString())
    val r2 =
        DbNoteRow(
            id = "2",
            userid = "u",
            title = "t2",
            content = "c2",
            date = LocalDateTime(2023, 1, 1, 0, 0).toString())
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(listOf(r1, r2)))
            .addHeader("Content-Type", "application/json"))

    val list = action.listNotes("u", page = 1, pageSize = 10)

    assertEquals(2, list.size)
    assertEquals("1", list[0].id)
    assertEquals(LocalDateTime.parse(r1.date), list[0].date)
  }

  @Test
  fun `updateNote returns note on success or null on failure`() = runBlocking {
    val note =
        Note(
            id = "1",
            userId = "u",
            title = "t",
            content = "c",
            date = LocalDateTime(2023, 1, 1, 0, 0))
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(DbResponse(success = true, error = null)))
            .addHeader("Content-Type", "application/json"))
    val updated = action.updateNote(note)
    assertNotNull(updated)
    assertEquals(note, updated)

    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(DbResponse(success = false, error = "err")))
            .addHeader("Content-Type", "application/json"))
    assertNull(action.updateNote(note))
  }

  @Test
  fun `deleteNote returns correct boolean for each scenario`() = runBlocking {
    val row =
        DbNoteRow(
            id = "1",
            userid = "u",
            title = "t",
            content = "c",
            date = LocalDateTime(2023, 1, 1, 0, 0).toString())
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(emptyList<DbNoteRow>()))
            .addHeader("Content-Type", "application/json"))
    assertFalse(action.deleteNote("1", "u"))

    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(listOf(row)))
            .addHeader("Content-Type", "application/json"))
    assertFalse(action.deleteNote("1", "other"))

    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(listOf(row)))
            .addHeader("Content-Type", "application/json"))
    server.enqueue(
        MockResponse()
            .setBody(Json.encodeToString(DbResponse(success = true, error = null)))
            .addHeader("Content-Type", "application/json"))
    assertTrue(action.deleteNote("1", "u"))
  }
}
