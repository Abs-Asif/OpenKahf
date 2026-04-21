package com.call.logger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM recording_contacts")
    fun getAllContacts(): Flow<List<RecordingContact>>

    @Query("SELECT * FROM recording_contacts WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getContactByNumber(phoneNumber: String): RecordingContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: RecordingContact)

    @Delete
    suspend fun deleteContact(contact: RecordingContact)
}
