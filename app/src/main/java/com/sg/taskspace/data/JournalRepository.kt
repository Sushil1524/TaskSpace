package com.sg.taskspace.data

import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    val allJournalEntries: Flow<List<JournalEntry>> = journalDao.getAllJournalEntries()

    fun getJournalEntryForDate(date: String): Flow<JournalEntry?> {
        return journalDao.getJournalEntryForDate(date)
    }

    suspend fun insertJournalEntry(entry: JournalEntry) {
        journalDao.insertJournalEntry(entry)
    }

    suspend fun deleteJournalEntry(entry: JournalEntry) {
        journalDao.deleteJournalEntry(entry)
    }

    suspend fun deleteJournalEntryById(entryId: String) {
        journalDao.deleteJournalEntryById(entryId)
    }

    suspend fun deleteAllJournalEntries() = journalDao.deleteAllJournalEntries()

    suspend fun getAllJournalEntriesSync(): List<JournalEntry> = journalDao.getAllJournalEntriesSync()

    // ── Soft Delete & Trash ──────────────────────────────────────────────────
    fun getDeletedJournalEntries(): Flow<List<JournalEntry>> = journalDao.getDeletedJournalEntries()

    suspend fun softDeleteJournalEntry(entryId: String) = journalDao.softDeleteJournalEntry(entryId)

    suspend fun restoreJournalEntry(entryId: String) = journalDao.restoreJournalEntry(entryId)

    suspend fun purgeExpiredJournalEntries(thresholdMillis: Long) = journalDao.purgeExpiredJournalEntries(thresholdMillis)

    suspend fun emptyTrashJournalEntries() = journalDao.emptyTrashJournalEntries()
}
