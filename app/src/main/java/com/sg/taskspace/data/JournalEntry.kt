package com.sg.taskspace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("date") val date: String, // YYYY-MM-DD
    @SerializedName("content") val content: String,
    @SerializedName("mood") val mood: String = "Neutral", // e.g., "Happy", "Neutral", "Sad"
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis()
)
