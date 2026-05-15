package com.jules.ideastracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarDialog : DialogFragment() {

    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val calendarView = CalendarView(requireContext())
        calendarView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

            // Show options: Start Timer or View History
            AlertDialog.Builder(requireContext())
                .setTitle("Date: $selectedDate")
                .setItems(arrayOf("Start Timer", "View History")) { _, which ->
                    if (which == 0) {
                        startTimer(selectedDate)
                    } else {
                        viewHistory(selectedDate)
                    }
                }
                .show()
        }

        return calendarView
    }

    private fun startTimer(date: String) {
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            putExtra("date", date)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
        Toast.makeText(context, "Timer started for $date. Click notification to stop.", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun viewHistory(date: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val history = AppDatabase.getDatabase(requireContext()).timerDao().getHistoryByDateSync(date)
            withContext(Dispatchers.Main) {
                if (history.isEmpty()) {
                    Toast.makeText(context, "No history for $date", Toast.LENGTH_SHORT).show()
                } else {
                    val sb = StringBuilder()
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    history.forEach {
                        val h = it.durationMillis / 3600000
                        val m = (it.durationMillis % 3600000) / 60000
                        val s = (it.durationMillis % 60000) / 1000
                        sb.append("${sdf.format(Date(it.timestamp))}: ${String.format("%02d:%02d:%02d", h, m, s)}\n")
                    }
                    AlertDialog.Builder(requireContext())
                        .setTitle("History for $date")
                        .setMessage(sb.toString())
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
