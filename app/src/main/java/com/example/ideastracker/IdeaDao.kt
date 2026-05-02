package com.example.ideastracker

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas WHERE status = :status ORDER BY CASE WHEN :isAsc = 1 THEN timestamp END ASC, CASE WHEN :isAsc = 0 THEN timestamp END DESC")
    fun getIdeasByStatus(status: IdeaStatus, isAsc: Int): LiveData<List<Idea>>

    @Insert
    suspend fun insert(idea: Idea)

    @Update
    suspend fun update(idea: Idea)

    @Delete
    suspend fun delete(idea: Idea)

    @Query("UPDATE ideas SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: Int, newStatus: IdeaStatus)
}
