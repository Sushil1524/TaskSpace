package com.sg.taskspace.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    fun getJournalEntryForDate(date: String): Flow<JournalEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteJournalEntry(entry: JournalEntry)
}
