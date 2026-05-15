package com.jules.ideastracker

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

enum class IdeaStatus {
    DONE, ONGOING, FUTURE
}

@Entity(tableName = "ideas")
data class Idea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val link: String?,
    val category: String? = null,
    val definitionOfDone: String? = null,
    val nextSteps: String? = null,
    val conclusion: String? = null,
    val status: IdeaStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val startedTimestamp: Long? = null,
    val finishedTimestamp: Long? = null,
    val lastSavedTimestamp: Long? = null,
    val manualOrder: Int = 0
) : Serializable

@Entity(tableName = "timer_history")
data class TimerHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format YYYY-MM-DD
    val durationMillis: Long,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dueDate: Long,
    val isPaid: Boolean = false
) : Serializable
