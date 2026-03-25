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
}
