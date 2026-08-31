package com.sg.taskspace.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE isDeleted = 0 ORDER BY date DESC")
    suspend fun getAllJournalEntriesSync(): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE isDeleted = 0 AND date = :date LIMIT 1")
    fun getJournalEntryForDate(date: String): Flow<JournalEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteJournalEntry(entry: JournalEntry)

    @Query("DELETE FROM journal_entries WHERE id = :entryId")
    suspend fun deleteJournalEntryById(entryId: String)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllJournalEntries()

    // ── Soft Delete & Trash ──────────────────────────────────────────────────
    @Query("SELECT * FROM journal_entries WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedJournalEntries(): Flow<List<JournalEntry>>

    @Query("UPDATE journal_entries SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :entryId")
    suspend fun softDeleteJournalEntry(entryId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE journal_entries SET isDeleted = 0, deletedAt = NULL WHERE id = :entryId")
    suspend fun restoreJournalEntry(entryId: String)

    @Query("DELETE FROM journal_entries WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :thresholdMillis")
    suspend fun purgeExpiredJournalEntries(thresholdMillis: Long)

    @Query("DELETE FROM journal_entries WHERE isDeleted = 1")
    suspend fun emptyTrashJournalEntries()
}
