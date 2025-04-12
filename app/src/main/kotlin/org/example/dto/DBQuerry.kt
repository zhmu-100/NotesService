package org.example.dto

import kotlinx.serialization.Serializable

/**
 * Запрос на создание записи в БД
 *
 * @property table Название таблицы
 * @property data Данные, которые нужно вставить
 */
@Serializable data class DbCreateRequest(val table: String, val data: Map<String, String>)

/**
 * Запрос на чтение данных из БД
 *
 * @property table Название таблицы
 * @property columns Список столбцов, которые необходимо прочитать, в данном случае читаем все
 * @property filters Фильтры для запроса
 */
@Serializable
data class DbReadRequest(
    val table: String,
    val columns: List<String> = listOf("*"),
    val filters: Map<String, String>? = null
)

/**
 * Запрос на обновление данных в БД.
 *
 * @property table Название таблицы
 * @property data Новые значения
 * @property condition Условие обновления
 * @property conditionParams Параметры для условия
 */
@Serializable
data class DbUpdateRequest(
    val table: String,
    val data: Map<String, String>,
    val condition: String,
    val conditionParams: List<String>
)

/**
 * Запрос на удаление записи из БД
 *
 * @property table Название таблицы
 * @property condition Условие удаления
 * @property conditionParams Параметры условия
 */
@Serializable
data class DbDeleteRequest(
    val table: String,
    val condition: String,
    val conditionParams: List<String>
)

/**
 * Ответ от БД на операции создания/обновления/удаления
 *
 * @property success Признак успешности
 * @property error Сообщение об ошибке, если есть
 */
@Serializable data class DbResponse(val success: Boolean? = null, val error: String? = null)
