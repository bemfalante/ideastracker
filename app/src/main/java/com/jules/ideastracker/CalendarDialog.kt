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
import android.widget.LinearLayout
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

            val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val isToday = selectedDate == now

            val options = if (isToday) arrayOf("Start Timer", "View History") else arrayOf("View History")

            AlertDialog.Builder(requireContext())
                .setTitle("Date: $selectedDate")
                .setItems(options) { _, which ->
                    if (isToday && which == 0) {
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

            withContext(Dispatchers.Main) {
                val layout = LinearLayout(requireContext())
                layout.orientation = LinearLayout.VERTICAL
                layout.setPadding(50, 20, 50, 20)

                val textView = TextView(requireContext())
                textView.text = "Select from ongoing ideas:"
                layout.addView(textView)

                val spinner = Spinner(requireContext())
                spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, titles)
                layout.addView(spinner)

                val textViewOr = TextView(requireContext())
                textViewOr.text = "\nOR enter a new task name:"
                layout.addView(textViewOr)

                val editText = EditText(requireContext())
                editText.hint = "New task name"
                layout.addView(editText)

                AlertDialog.Builder(requireContext())
                    .setTitle("Select Task for $date")
                    .setView(layout)
                    .setPositiveButton("Start") { _, _ ->
                        val newName = editText.text.toString()
                        if (newName.isNotBlank()) {
                            startTimer(date, newName)
                        } else if (titles.isNotEmpty()) {
                            startTimer(date, spinner.selectedItem.toString())
                        } else {
                            Toast.makeText(context, "Please provide a task name", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
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
                    var totalMillis = 0L

                    history.forEach {
                        totalMillis += it.durationMillis
                        // Rule 1: Show hours, minutes, seconds
                        val h = it.durationMillis / 3600000
                        val m = (it.durationMillis % 3600000) / 60000
                        val s = (it.durationMillis % 60000) / 1000
                        val task = if (it.taskName != null) " [${it.taskName}]" else ""
                        sb.append("${String.format("%02d:%02d:%02d", h, m, s)}$task\n")
                    }

                    // Rule 1: Total Sum only hours and minutes
                    val totalMinutes = totalMillis / 60000
                    val hSum = totalMinutes / 60
                    val mSum = totalMinutes % 60
                    val totalString = "TOTAL TIME: ${String.format("%02dh:%02dm", hSum, mSum)}\n\n"

                    AlertDialog.Builder(requireContext())
                        .setTitle("History for $date")
                        .setMessage(totalString + sb.toString())
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
