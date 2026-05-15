package com.jules.ideastracker

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas WHERE status = 'FUTURE' ORDER BY manualOrder ASC, CASE WHEN :isAsc = 1 THEN timestamp END ASC, CASE WHEN :isAsc = 0 THEN timestamp END DESC")
    fun getFutureIdeas(isAsc: Int): LiveData<List<Idea>>

    @Query("SELECT * FROM ideas WHERE status = 'ONGOING' ORDER BY manualOrder ASC, CASE WHEN :isAsc = 1 THEN startedTimestamp END ASC, CASE WHEN :isAsc = 0 THEN startedTimestamp END DESC")
    fun getOngoingIdeas(isAsc: Int): LiveData<List<Idea>>

    @Query("SELECT * FROM ideas WHERE status = 'DONE' ORDER BY manualOrder ASC, CASE WHEN :isAsc = 1 THEN finishedTimestamp END ASC, CASE WHEN :isAsc = 0 THEN finishedTimestamp END DESC")
    fun getDoneIdeas(isAsc: Int): LiveData<List<Idea>>

    @Query("SELECT MIN(manualOrder) FROM ideas")
    suspend fun getMinManualOrder(): Int?

    @Query("SELECT * FROM ideas WHERE status = 'ONGOING'")
    suspend fun getOngoingIdeasSync(): List<Idea>

    @Query("SELECT * FROM ideas WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND status = 'FUTURE' ORDER BY manualOrder ASC, CASE WHEN :isAsc = 1 THEN timestamp END ASC, CASE WHEN :isAsc = 0 THEN timestamp END DESC")
    fun searchFutureIdeas(query: String, isAsc: Int): LiveData<List<Idea>>

    @Query("SELECT * FROM ideas WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND status = 'ONGOING' ORDER BY manualOrder ASC, CASE WHEN :isAsc = 1 THEN startedTimestamp END ASC, CASE WHEN :isAsc = 0 THEN startedTimestamp END DESC")
    fun searchOngoingIdeas(query: String, isAsc: Int): LiveData<List<Idea>>

    @Query("SELECT * FROM ideas WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND status = 'DONE' ORDER BY manualOrder ASC, CASE WHEN :isAsc = 1 THEN finishedTimestamp END ASC, CASE WHEN :isAsc = 0 THEN finishedTimestamp END DESC")
    fun searchDoneIdeas(query: String, isAsc: Int): LiveData<List<Idea>>

    @Insert
    suspend fun insert(idea: Idea): Long

    @Update
    suspend fun update(idea: Idea)

    @Delete
    suspend fun delete(idea: Idea)
}
