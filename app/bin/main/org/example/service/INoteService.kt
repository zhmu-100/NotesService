package org.example.service

import org.example.model.Note

/**
 * Интерфейс **бизнес** логики работы с заметками.
 *
 * Доступные операции
 * - Создать заметку
 * - Получить заметку по ID
 * - Получить список заметок пользователя
 * - Обновить заметку
 * - Удалить заметку
 */
interface INoteService {
  /**
   * Создать новую заметку
   *
   * @param note Заметка для создания
   * @return Созданная заметка
   */
  suspend fun createNote(note: Note): Note

  /**
   * Получает заметку по ID
   *
   * @param id ID заметки
   * @return Заметка или null, если не найдена
   */
  suspend fun getNote(id: String): Note?

  /**
   * Возвращает List<Note> заметок для пользователя с постраничной навигацией
   *
   * @param userId ID пользователя
   * @param page Номер страницы (начиная с 1)
   * @param pageSize Количество заметок на страницу
   * @return Список заметок
   */
  suspend fun listNotes(userId: String, page: Int, pageSize: Int): List<Note>

  /**
   * Обновляет существующую заметку
   *
   * @param note Обновленная заметка (обновление на основе ID)
   * @return Обновленная заметка или null, если заметка не найдена
   */
  suspend fun updateNote(note: Note): Note?

  /**
   * Удаляет заметку
   *
   * @param id ID заметки
   * @param userId ID пользователя, которому принадлежит заметка
   * @return true, если заметка успешно удалена, иначе false
   */
  suspend fun deleteNote(id: String, userId: String): Boolean
}
