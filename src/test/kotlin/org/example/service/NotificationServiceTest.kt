package org.example.service

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.example.actions.INotificationAction
import org.example.model.Notification
import org.example.model.NotificationActionEnum
import org.example.model.NotificationSnoozeEnum
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NotificationServiceTest {
  private lateinit var action: INotificationAction
  private lateinit var service: NotificationService

  @BeforeEach
  fun setUp() {
    action = mockk()
    service = NotificationService(action)
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `createNotification sets id and createDate then delegates`() = runBlocking {
    val input =
        Notification(
            id = "",
            title = "Test Title",
            description = "Test Description",
            notificationDate = LocalDateTime(1970, 1, 1, 0, 0),
            createDate = LocalDateTime(1970, 1, 1, 0, 0),
            userId = "user1")
    val slot = slot<Notification>()
    coEvery { action.createNotification(capture(slot)) } answers { slot.captured }

    val result = service.createNotification(input)

    assertTrue(result.id.isNotBlank(), "Expected non-empty id")
    assertNotNull(result.createDate, "Expected non-null createDate")
    assertEquals(input.title, result.title)
    assertEquals(input.description, result.description)
    assertEquals(input.notificationDate, result.notificationDate)
    assertEquals(input.userId, result.userId)
    coVerify(exactly = 1) { action.createNotification(any()) }
  }

  @Test
  fun `getNotification returns value from action`() = runBlocking {
    val expected =
        Notification(
            id = "notif1",
            title = "Title1",
            description = "Desc1",
            notificationDate = LocalDateTime(2023, 1, 1, 12, 0),
            createDate = LocalDateTime(2023, 1, 1, 12, 0),
            userId = "user1")
    coEvery { action.getNotification(expected.id) } returns expected

    val result = service.getNotification(expected.id)

    assertEquals(expected, result)
    coVerify { action.getNotification(expected.id) }
  }

  @Test
  fun `listNotifications delegates to action`() = runBlocking {
    val n1 =
        Notification(
            id = "1",
            title = "T1",
            description = "D1",
            notificationDate = LocalDateTime(2023, 1, 1, 0, 0),
            createDate = LocalDateTime(2023, 1, 1, 0, 0),
            userId = "user1")
    val n2 =
        Notification(
            id = "2",
            title = "T2",
            description = "D2",
            notificationDate = LocalDateTime(2023, 1, 2, 0, 0),
            createDate = LocalDateTime(2023, 1, 2, 0, 0),
            userId = "user1")
    val expectedList = listOf(n1, n2)
    coEvery { action.listNotifications("user1", 1, 10) } returns expectedList

    val result = service.listNotifications("user1", 1, 10)

    assertEquals(expectedList, result)
    coVerify { action.listNotifications("user1", 1, 10) }
  }

  @Test
  fun `performAction delegates and returns Notification`() = runBlocking {
    val id = "1"
    val userId = "user1"
    val actionTypes = NotificationActionEnum.values()
    val snoozeOptions = NotificationSnoozeEnum.values()
    val actionType = actionTypes.first()
    val snooze = snoozeOptions.firstOrNull()
    val expected =
        Notification(
            id = id,
            title = "Title1",
            description = "Desc1",
            notificationDate = LocalDateTime(2023, 1, 1, 0, 0),
            createDate = LocalDateTime(2023, 1, 1, 0, 0),
            userId = userId)
    coEvery { action.performNotificationAction(id, userId, actionType, snooze) } returns expected

    val result = service.performAction(id, userId, actionType, snooze)

    assertEquals(expected, result)
    coVerify { action.performNotificationAction(id, userId, actionType, snooze) }
  }
}
