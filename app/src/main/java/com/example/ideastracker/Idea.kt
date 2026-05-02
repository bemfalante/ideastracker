package com.example.ideastracker

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class IdeaStatus {
    DONE, ONGOING, FUTURE
}

@Entity(tableName = "ideas")
data class Idea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val link: String?,
    val status: IdeaStatus,
    val timestamp: Long = System.currentTimeMillis()
)
