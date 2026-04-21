package com.call.logger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_contacts")
data class RecordingContact(
    @PrimaryKey val phoneNumber: String,
    val name: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
