package org.example.actions

import org.example.model.Note
import java.util.concurrent.ConcurrentHashMap


/**
 * Реализация интерфейса [INoteAction]
 *
 * @see INoteAction
 */
class NoteAction : INoteAction {

  private val notes = ConcurrentHashMap<String, Note>() // mock

  override suspend fun createNote(note: Note): Note {
    notes[note.id] = note
    println("Created note: $note")
    return note
  }

  override suspend fun getNote(id: String): Note? {
    println("Retrieving note with id: $id")
    return notes[id]
  }

  override suspend fun listNotes(userId: String, page: Int, pageSize: Int): List<Note> {
    println("Listing notes for user: $userId, page: $page, pageSize: $pageSize")
    return notes.values.filter { it.userId == userId }
      .sortedByDescending { it.date }
      .drop((page - 1) * pageSize)
      .take(pageSize)
  }

  override suspend fun updateNote(note: Note): Note? {
    println("Before update: $notes[note.id]")
    println("Updating note: $note")
    return if (notes.containsKey(note.id)) {
      notes[note.id] = note
      note
    } else {
      null
    }
  }

  override suspend fun deleteNote(id: String, userId: String): Boolean {
    println("Deleting note $id for user: $userId")
    return notes.remove(id)?.let {it.userId == userId} ?: false
  }
}