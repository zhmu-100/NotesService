package org.example.service

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.example.actions.INoteAction
import org.example.model.Note
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NoteServiceTest {
  private lateinit var actions: INoteAction
  private lateinit var service: NoteService

  @BeforeEach
  fun setUp() {
    actions = mockk()
    service = NoteService(actions)
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `createNote should assign id and date, then delegate to actions`() = runBlocking {
    val input =
        Note(
            id = "",
            date = LocalDateTime(1970, 1, 1, 0, 0),
            title = "Test",
            content = "Content",
            userId = "user1")
    val slot = mutableListOf<Note>()
    coEvery { actions.createNote(capture(slot)) } answers { slot.first() }

    val result = service.createNote(input)

    assertTrue(result.id.isNotBlank(), "Expected non-empty id")
    assertNotNull(result.date, "Expected non-null date")
    assertEquals(input.title, result.title)
    assertEquals(input.content, result.content)
    assertEquals(input.userId, result.userId)
    coVerify(exactly = 1) { actions.createNote(any()) }
  }

  @Test
  fun `getNote should return note from actions`() = runBlocking {
    val expected =
        Note(
            id = "note1",
            date = LocalDateTime(2023, 1, 1, 12, 0),
            title = "Title",
            content = "Content",
            userId = "user1")
    coEvery { actions.getNote(expected.id) } returns expected

    val result = service.getNote(expected.id)

    assertEquals(expected, result)
    coVerify { actions.getNote(expected.id) }
  }

  @Test
  fun `listNotes should delegate to actions`() = runBlocking {
    val note1 =
        Note(
            id = "1",
            date = LocalDateTime(2023, 1, 1, 0, 0),
            title = "T1",
            content = "C1",
            userId = "user1")
    val note2 =
        Note(
            id = "2",
            date = LocalDateTime(2023, 1, 2, 0, 0),
            title = "T2",
            content = "C2",
            userId = "user1")
    val expectedList = listOf(note1, note2)
    coEvery { actions.listNotes("user1", 1, 10) } returns expectedList

    val result = service.listNotes("user1", 1, 10)

    assertEquals(expectedList, result)
    coVerify { actions.listNotes("user1", 1, 10) }
  }

  @Test
  fun `updateNote should update date and delegate to actions`() = runBlocking {
    val original =
        Note(
            id = "1",
            date = LocalDateTime(2020, 1, 1, 0, 0),
            title = "Old",
            content = "OldC",
            userId = "user1")
    val slot = mutableListOf<Note>()
    coEvery { actions.updateNote(capture(slot)) } answers { slot.first() }

    val result = service.updateNote(original)

    assertNotNull(result, "Expected non-null result")
    assertEquals(original.id, result!!.id)
    assertEquals(original.title, result.title)
    assertEquals(original.content, result.content)
    assertNotEquals(original.date, result.date, "Expected date to be updated")
    coVerify(exactly = 1) { actions.updateNote(any()) }
  }

  @Test
  fun `deleteNote should return result from actions`() = runBlocking {
    coEvery { actions.deleteNote(id = "1", userId = "user1") } returns true

    val result = service.deleteNote(id = "1", userId = "user1")

    assertTrue(result)
    coVerify { actions.deleteNote(id = "1", userId = "user1") }
  }
}
