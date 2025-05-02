package org.example

import com.mad.client.LoggerClient
import com.mad.model.LogLevel
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.example.actions.NoteAction
import org.example.actions.NotificationAction
import org.example.router.registerNoteRoutes
import org.example.router.registerNotificationRoutes
import org.example.service.NoteService
import org.example.service.NotificationService

/** Интерфейс для логирования, который будет реализован как LoggerClient, так и заглушкой */
interface ILogger {
  fun logActivity(
      event: String,
      userId: String? = null,
      deviceModel: String? = null,
      level: LogLevel = LogLevel.INFO,
      additionalData: Map<String, String> = emptyMap()
  )

  fun logError(
      event: String,
      errorMessage: String,
      userId: String? = null,
      deviceModel: String? = null,
      stackTrace: String? = null,
      level: LogLevel = LogLevel.ERROR
  )
}

/** Адаптер для LoggerClient, реализующий интерфейс ILogger */
class LoggerAdapter(private val client: LoggerClient) : ILogger {
  override fun logActivity(
      event: String,
      userId: String?,
      deviceModel: String?,
      level: LogLevel,
      additionalData: Map<String, String>
  ) {
    try {
      client.logActivity(event, userId, deviceModel, level, additionalData)
    } catch (e: Exception) {
      System.err.println("Ошибка при логировании активности: ${e.message}")
      e.printStackTrace()
    }
  }

  override fun logError(
      event: String,
      errorMessage: String,
      userId: String?,
      deviceModel: String?,
      stackTrace: String?,
      level: LogLevel
  ) {
    try {
      client.logError(event, errorMessage, userId, deviceModel, stackTrace, level)
    } catch (e: Exception) {
      System.err.println("Ошибка при логировании ошибки: ${e.message}")
      e.printStackTrace()
    }
  }
}

/** Заглушка логгера, которая просто выводит сообщения в консоль */
class ConsoleLogger : ILogger {
  override fun logActivity(
      event: String,
      userId: String?,
      deviceModel: String?,
      level: LogLevel,
      additionalData: Map<String, String>
  ) {
    System.out.println("LOG: $event | User: $userId | Level: $level | Data: $additionalData")
  }

  override fun logError(
      event: String,
      errorMessage: String,
      userId: String?,
      deviceModel: String?,
      stackTrace: String?,
      level: LogLevel
  ) {
    System.err.println("ERROR: $event | Message: $errorMessage | User: $userId | Level: $level")
    stackTrace?.let { System.err.println("Stack trace: $it") }
  }
}

/**
 * Точка входа в приложение
 *
 * Дефолтный порт - 8001
 */
fun main() {
  val dotenv = dotenv { ignoreIfMissing = true }
  val port = dotenv["SERVICE_PORT"]?.toIntOrNull() ?: 8001

  // Инициализация логгера
  val loggerHost = dotenv["REDIS_HOST"] ?: "localhost"
  val loggerPort = dotenv["REDIS_PORT"]?.toIntOrNull() ?: 6379
  val loggerPassword = dotenv["REDIS_PASSWORD"] ?: ""

  // Создаем логгер с обработкой ошибок
  val logger = createSafeLogger(loggerHost, loggerPort, loggerPassword)

  // Кэш для дедупликации ошибок
  val errorCache = ConcurrentHashMap<String, AtomicInteger>()

  try {
    logger.logActivity(
        event = "Application starting",
        additionalData = mapOf("port" to port.toString(), "redisHost" to loggerHost))

    // Настраиваем глобальный перехватчик необработанных исключений
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      // Получаем хеш ошибки для дедупликации
      val errorHash = "${throwable.javaClass.name}:${throwable.message}"

      // Проверяем, не логировали ли мы уже эту ошибку недавно
      val errorCount = errorCache.computeIfAbsent(errorHash) { AtomicInteger(0) }
      val count = errorCount.incrementAndGet()

      // Если это первое появление ошибки или кратно 10, логируем
      if (count == 1 || count % 10 == 0) {
        // Получаем стек вызовов
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        logger.logError(
            event = "Необработанное исключение в потоке: ${thread.name} (повторений: $count)",
            errorMessage = throwable.message ?: "Неизвестная ошибка",
            stackTrace = stackTrace,
            level = LogLevel.ERROR)
      }
    }

    embeddedServer(Netty, port = port) {
          install(ContentNegotiation) { json() }

          // Добавляем перехватчик для всех запросов
          intercept(ApplicationCallPipeline.Monitoring) {
            // Генерируем уникальный ID для запроса
            val requestId = UUID.randomUUID().toString()

            try {
              // Логируем начало запроса
              logger.logActivity(
                  event = "Запрос начат: ${call.request.httpMethod.value} ${call.request.path()}",
                  additionalData =
                      mapOf(
                          "requestId" to requestId,
                          "userAgent" to (call.request.headers["User-Agent"] ?: "unknown")))

              // Продолжаем обработку запроса
              proceed()

              // Логируем успешное завершение запроса
              logger.logActivity(
                  event =
                      "Запрос завершен: ${call.request.httpMethod.value} ${call.request.path()}",
                  additionalData =
                      mapOf(
                          "requestId" to requestId,
                          "status" to (call.response.status()?.value?.toString() ?: "unknown")))
            } catch (e: Exception) {
              // Получаем хеш ошибки для дедупликации
              val errorHash = "${e.javaClass.name}:${e.message}"

              // Проверяем, не логировали ли мы уже эту ошибку недавно
              val errorCount = errorCache.computeIfAbsent(errorHash) { AtomicInteger(0) }
              val count = errorCount.incrementAndGet()

              // Если это первое появление ошибки или кратно 10, логируем
              if (count == 1 || count % 10 == 0) {
                // Получаем стек вызовов
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()

                // Логируем ошибку
                logger.logError(
                    event =
                        "Ошибка при обработке запроса: ${call.request.httpMethod.value} ${call.request.path()} (повторений: $count)",
                    errorMessage = e.message ?: "Неизвестная ошибка",
                    stackTrace = stackTrace,
                    level = LogLevel.ERROR)
              }

              // Пробрасываем исключение дальше
              throw e
            }
          }

          // notes
          val noteAction = NoteAction()
          val noteService = NoteService(noteAction)
          registerNoteRoutes(noteService, logger)

          // notifications
          val notificationAction = NotificationAction()
          val notificationService = NotificationService(notificationAction)
          registerNotificationRoutes(notificationService, logger)

          // Добавляем обработчик для перехвата необработанных исключений
          environment.monitor.subscribe(ApplicationStopping) {
            logger.logActivity(event = "Приложение останавливается", level = LogLevel.WARN)
          }

          // Добавляем обработчик для перехвата ошибок на уровне приложения
          environment.monitor.subscribe(ApplicationStarted) {
            // Логируем успешный запуск
            logger.logActivity(
                event = "Application started successfully",
                additionalData = mapOf("port" to port.toString()))
          }

          // Добавляем обработчик для перехвата ошибок на уровне приложения
          environment.monitor.subscribe(ApplicationStarting) {
            // Логируем начало запуска
            logger.logActivity(
                event = "Application starting phase",
                additionalData = mapOf("port" to port.toString()))
          }

          // Добавляем обработчик для перехвата ошибок на уровне приложения
          environment.monitor.subscribe(ApplicationStopPreparing) {
            // Логируем подготовку к остановке
            logger.logActivity(event = "Application preparing to stop", level = LogLevel.WARN)
          }

          // Добавляем обработчик для перехвата ошибок на уровне приложения
          environment.monitor.subscribe(ApplicationStopped) {
            // Логируем остановку
            logger.logActivity(event = "Application stopped", level = LogLevel.WARN)
          }
        }
        .start(wait = true)
  } catch (e: Exception) {
    // Получаем стек вызовов
    val sw = StringWriter()
    e.printStackTrace(PrintWriter(sw))
    val stackTrace = sw.toString()

    logger.logError(
        event = "Критическая ошибка при запуске приложения",
        errorMessage = e.message ?: "Неизвестная ошибка",
        stackTrace = stackTrace,
        level = LogLevel.FATAL)
    throw e
  }
}

/**
 * Создает экземпляр ILogger с обработкой ошибок для предотвращения сбоев приложения при проблемах с
 * Redis
 */
private fun createSafeLogger(host: String, port: Int, password: String): ILogger {
  // Создаем обычный экземпляр LoggerClient
  return try {
    val client = LoggerClient(host = host, port = port, password = password)
    LoggerAdapter(client)
  } catch (e: Exception) {
    System.err.println("Ошибка при создании LoggerClient: ${e.message}")
    e.printStackTrace()
    // Возвращаем заглушку в случае ошибки
    ConsoleLogger()
  }
}
