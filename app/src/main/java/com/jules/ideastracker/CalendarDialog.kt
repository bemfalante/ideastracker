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
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
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

            AlertDialog.Builder(requireContext())
                .setTitle("Date: $selectedDate")
                .setItems(arrayOf("Start Timer", "View History")) { _, which ->
                    if (which == 0) {
                        promptForTaskName(selectedDate)
                    } else {
                        viewHistory(selectedDate)
                    }
                }
                .show()
        }

        return calendarView
    }

    private fun promptForTaskName(date: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            val ongoingIdeas = db.ideaDao().getOngoingIdeasSync()
            val titles = ongoingIdeas.map { it.title }.toMutableList()
            titles.add(0, "Create New Task Name...")

            withContext(Dispatchers.Main) {
                val spinner = Spinner(requireContext())
                spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, titles)

                AlertDialog.Builder(requireContext())
                    .setTitle("Select Task for $date")
                    .setView(spinner)
                    .setPositiveButton("Start") { _, _ ->
                        val selected = spinner.selectedItem.toString()
                        if (selected == "Create New Task Name...") {
                            // Rule 4: prompt for name immediately
                            promptNewTaskName(date)
                        } else {
                            startTimer(date, selected)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun promptNewTaskName(date: String) {
        val editText = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("New Task Name")
            .setView(editText)
            .setPositiveButton("Start") { _, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    startTimer(date, name)
                } else {
                    Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startTimer(date: String, taskName: String) {
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            putExtra("date", date)
            putExtra("taskName", taskName)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
        Toast.makeText(context, "Timer started. Click notification to stop.", Toast.LENGTH_SHORT).show()
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
                        // Rule 5: Show minutes and seconds only
                        val totalSeconds = it.durationMillis / 1000
                        val minutes = totalSeconds / 60
                        val seconds = totalSeconds % 60
                        val timeString = String.format("%02d:%02d", minutes, seconds)
                        val task = if (it.taskName != null) " [${it.taskName}]" else ""
                        sb.append("${sdf.format(Date(it.timestamp))}: $timeString$task\n")
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
