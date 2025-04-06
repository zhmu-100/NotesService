package org.example.actions

import org.example.model.Note

/**
 * Интерфейс для работы с заметками
 *
 * Возможные операции:
 * - Создание заметки
 * - Получение заметки по id
 * - Получение списка заметок для заданного пользователя
 * - Обновление заметки
 * - Удаление заметки
 */
interface INoteAction {
  /**
   * Создает новую заметку
   *
   * @param note Заметка для сохранения
   * @return Сохраненная заметка
   */
  suspend fun createNote(note: Note): Note

  /**
   * Получает заметку по ее ID
   *
   * @param id ID заметки
   * @return Заметка или null, если она не найдена
   */
  suspend fun getNote(id: String): Note?

  /**
   * Получает список всех заметок для пользователя с учетом пейджинга
   *
   * @param userId ID пользователя
   * @param page Номер страницы (начиная с 1)
   * @param pageSize Количество записей на странице
   * @return List заметок
   */
  suspend fun listNotes(userId: String, page: Int, pageSize: Int): List<Note>

  /**
   * Обновляет существующую заметку
   *
   * @param note Обновленная заметка
   * @return Note или null, если заметка не была найдена\обновлена
   */
  suspend fun updateNote(note: Note): Note?

  /**
   * Удаляет заметку
   *
   * @param id ID заметки
   * @param userId ID пользователя
   * @return true, если удалена, иначе false
   */
  suspend fun deleteNote(id: String, userId: String): Boolean
}