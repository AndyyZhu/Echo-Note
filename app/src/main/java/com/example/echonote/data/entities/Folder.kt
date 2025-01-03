package com.example.echonote.data.entities

import kotlinx.datetime.*
import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    val id: Long,
    val user_id: String,
    var title: String,
    var description: String?,
    val created_on: LocalDateTime,
    var updated_on: LocalDateTime,
)
