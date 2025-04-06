package org.example.service

import org.example.model.Note
import org.example.actions.INoteAction
import org.example.actions.NoteAction
import java.time.Instant
import java.util.UUID

/**
 * Реализация интерфейса [INoteService] для бизнес логики работы с заметками. Существование данного класса под вопросом, но наверное хорошо разделять обращения к БД из [NoteAction] и бизнес логику...хотя из бизнес логики здесь 2 строки...зачем я это пишу...я не понимаю
 */
class NoteService(private val actions: INoteAction = NoteAction()) : INoteService {

  /**
   * Создаент новую заметку. Из бизнес логики здесь те самые 2 строки, создание UUID и даты...
   *
   * @param note Заметка для создания (без даты и UUID)
   * @return Созданная заметка с UUID и датой
   */
  override suspend fun createNote(note: Note): Note {
    val newNote = note.copy(
      id = UUID.randomUUID().toString(),
      date = Instant.now()
    )
    return actions.createNote(newNote)
  }

  /**
   * Получает заметку по ее ID
   *
   * @param id ID заметки
   * @return Заметка или null, если она не найдена
   */
  override suspend fun getNote(id: String): Note? {
    return actions.getNote(id)
  }

  /**
   * Получает List<Note> заметок для пользователя с постраничной навигацией
   *
   * @param userId ID пользователя
   * @param page Номер страницы (начиная с 1)
   * @param pageSize Количество заметок на страницу
   * @return Список заметок
   */
  override suspend fun listNotes(userId: String, page: Int, pageSize: Int): List<Note> {
    return actions.listNotes(userId, page, pageSize)
  }

  /**
   * Обновляет существующую заметку. А вот еще бизнес логика, целая одна строка. Вау.... Обновляем дату, круто...
   *
   * @param note Обновленная заметка. ID должен совпадать
   * @return Обновленная заметка
   */
  override suspend fun updateNote(note: Note): Note? {
    val updatedNote = note.copy(date = Instant.now())
    return actions.updateNote(updatedNote)
  }

  /**
   * Удаляет существующую заметку на основе ее id и id пользователя
   *
   * @param id ID заметки
   * @param userId ID пользователя
   * @return true, если заметка была удалена, иначе false
   */
  override suspend fun deleteNote(id: String, userId: String): Boolean {
    return actions.deleteNote(id, userId)
  }
}