package com.example.ideastracker

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
    val status: IdeaStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val startedTimestamp: Long? = null,
    val finishedTimestamp: Long? = null
) : Serializable
