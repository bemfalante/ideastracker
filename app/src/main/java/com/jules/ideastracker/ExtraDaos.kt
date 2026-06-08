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

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events WHERE date = :date ORDER BY timestamp ASC")
    fun getEventsByDate(date: String): LiveData<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getEventsByDateSync(date: String): List<CalendarEvent>

    @Query("SELECT * FROM calendar_events ORDER BY date ASC")
    suspend fun getAllEventsSync(): List<CalendarEvent>

    @Insert
    suspend fun insert(event: CalendarEvent)

    @Delete
    suspend fun delete(event: CalendarEvent)
}
