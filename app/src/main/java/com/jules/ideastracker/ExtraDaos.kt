package com.jules.ideastracker

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY dueDate ASC")
    fun getAllBills(): LiveData<List<Bill>>

    @Insert
    suspend fun insert(bill: Bill)

    @Update
    suspend fun update(bill: Bill)

    @Delete
    suspend fun delete(bill: Bill)
}

@Dao
interface TimerDao {
    @Query("SELECT * FROM timer_history WHERE date = :date ORDER BY timestamp DESC")
    fun getHistoryByDate(date: String): LiveData<List<TimerHistory>>

    @Query("SELECT * FROM timer_history WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getHistoryByDateSync(date: String): List<TimerHistory>

    @Insert
    suspend fun insert(history: TimerHistory)
}
