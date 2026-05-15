package com.jules.ideastracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerService : Service() {

    private var startTime = 0L
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var date: String? = null
    private var taskName: String? = null

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                val elapsed = System.currentTimeMillis() - startTime
                updateNotification(elapsed)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopTimer()
        } else {
            date = intent?.getStringExtra("date")
            taskName = intent?.getStringExtra("taskName")
            startTimer()
        }
        return START_STICKY
    }

    private fun startTimer() {
        if (!isRunning) {
            startTime = System.currentTimeMillis()
            isRunning = true
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification(0))
            handler.post(updateRunnable)
        }
    }

    private fun stopTimer() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        val duration = System.currentTimeMillis() - startTime

        date?.let { d ->
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getDatabase(applicationContext).timerDao().insert(
                    TimerHistory(date = d, durationMillis = duration, taskName = taskName)
                )
            }
        }

        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(elapsed: Long): android.app.Notification {
        val stopIntent = Intent(this, TimerService::class.java).apply { action = "STOP" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val hours = (elapsed / 3600000)
        val minutes = (elapsed % 3600000) / 60000
        val seconds = (elapsed % 60000) / 1000
        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        val content = "Task: ${taskName ?: "Unnamed"}\nElapsed Time: $timeString"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cronômetro Running")
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentIntent(stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(elapsed: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(elapsed))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Timer Channel", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1
    }
}
