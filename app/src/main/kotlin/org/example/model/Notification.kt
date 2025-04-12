package org.example.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String = "",
    val userId: String,
    val title: String,
    val description: String,
    val createDate: LocalDateTime,
    val notificationDate: LocalDateTime
)
