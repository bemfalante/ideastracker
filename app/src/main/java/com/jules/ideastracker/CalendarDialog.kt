package com.jules.ideastracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val calendarView = CalendarView(requireContext())
        calendarView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvEventsWarning = TextView(requireContext()).apply {
            setPadding(32, 16, 32, 16)
            textSize = 14f
            visibility = View.GONE
            setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
        }

        root.addView(calendarView)
        root.addView(tvEventsWarning)

        val updateEventsWarning = { year: Int, month: Int ->
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(requireContext())
                val allEvents = db.calendarEventDao().getAllEventsSync()

                // Filter for selected year and month
                val targetMonth = String.format("%04d-%02d", year, month + 1)
                val datesWithEvents = allEvents
                    .filter { it.date.startsWith(targetMonth) }
                    .map { it.date }
                    .distinct()
                    .sorted()

                withContext(Dispatchers.Main) {
                    if (datesWithEvents.isNotEmpty()) {
                        val formattedDates = datesWithEvents.map { dateStr ->
                            try {
                                val parts = dateStr.split("-")
                                "${parts[2]}/${parts[1]}"
                            } catch (e: Exception) {
                                dateStr
                            }
                        }.joinToString(", ")
                        tvEventsWarning.text = "Days with events: $formattedDates"
                        tvEventsWarning.visibility = View.VISIBLE
                    } else {
                        tvEventsWarning.visibility = View.GONE
                    }
                }
            }
        }

        // Initial call for the current date
        val nowCal = Calendar.getInstance()
        updateEventsWarning(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH))

        // Monitor for month changes by finding the next/previous buttons or relying on a frequent check
        // Standard CalendarView is a bit tricky. We'll try to find buttons by content description.
        root.post {
            val findButtons = { view: View ->
                val out = mutableListOf<View>()
                val queue = mutableListOf(view)
                while (queue.isNotEmpty()) {
                    val v = queue.removeAt(0)
                    if (v.contentDescription?.toString()?.contains("month", ignoreCase = true) == true ||
                        v.contentDescription?.toString()?.contains("mês", ignoreCase = true) == true) {
                        out.add(v)
                    }
                    if (v is ViewGroup) {
                        for (i in 0 until v.childCount) {
                            queue.add(v.getChildAt(i))
                        }
                    }
                }
                out
            }

            findButtons(calendarView).forEach { button ->
                button.setOnClickListener {
                    button.performClick() // Ensure original behavior
                    root.postDelayed({
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = calendarView.date
                        updateEventsWarning(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                    }, 100)
                }
            }
        }

        // Fallback: poll for date changes every 500ms while the dialog is visible
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val pollTask = object : Runnable {
            var lastMonth = -1
            override fun run() {
                if (!isAdded) return
                val cal = Calendar.getInstance()
                cal.timeInMillis = calendarView.date
                val currentMonth = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
                if (currentMonth != lastMonth) {
                    lastMonth = currentMonth
                    updateEventsWarning(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(pollTask)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            updateEventsWarning(year, month)
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

            val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val isToday = selectedDate == now

            val options = mutableListOf<String>()
            if (isToday) options.add("Start Timer")
            options.add("Add Event")
            options.add("View History")

            AlertDialog.Builder(requireContext())
                .setTitle("Date: $selectedDate")
                .setItems(options.toTypedArray()) { _, which ->
                    when (options[which]) {
                        "Start Timer" -> promptForTaskName(selectedDate)
                        "Add Event" -> promptForEvent(selectedDate)
                        "View History" -> viewHistory(selectedDate)
                    }
                }
                .show()
        }

        return root
    }

    private fun promptForEvent(date: String) {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 20)

        val etTitle = EditText(requireContext())
        etTitle.hint = "Event Title"
        layout.addView(etTitle)

        val etHour = EditText(requireContext())
        etHour.hint = "Hour (optional, e.g. 14:30)"
        layout.addView(etHour)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Event to $date")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString()
                val hour = etHour.text.toString().takeIf { it.isNotBlank() }
                if (title.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        AppDatabase.getDatabase(requireContext()).calendarEventDao().insert(
                            CalendarEvent(date = date, title = title, hour = hour)
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

                val textViewOr = TextView(requireContext())
                textViewOr.text = "Enter a task name:"
                layout.addView(textViewOr)

                val editText = EditText(requireContext())
                editText.hint = "New task name"
                layout.addView(editText)

                val textView = TextView(requireContext())
                textView.text = "\nOR select from ongoing ideas:"
                layout.addView(textView)

                val spinner = Spinner(requireContext())
                spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, titles)
                layout.addView(spinner)

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
            val db = AppDatabase.getDatabase(requireContext())
            val timerHistory = db.timerDao().getHistoryByDateSync(date)
            val events = db.calendarEventDao().getEventsByDateSync(date)

            withContext(Dispatchers.Main) {
                val sb = StringBuilder()

                if (events.isNotEmpty()) {
                    sb.append("--- EVENTS ---\n")
                    events.forEach {
                        val h = if (it.hour != null) "[${it.hour}] " else ""
                        sb.append("$h${it.title}\n")
                    }
                    sb.append("\n")
                }

                if (timerHistory.isNotEmpty()) {
                    sb.append("--- TASKS ---\n")
                    var totalMillis = 0L
                    timerHistory.forEach {
                        totalMillis += it.durationMillis
                        val h = it.durationMillis / 3600000
                        val m = (it.durationMillis % 3600000) / 60000
                        val s = (it.durationMillis % 60000) / 1000
                        val task = if (it.taskName != null) " [${it.taskName}]" else ""
                        sb.append("${String.format("%02d:%02d:%02d", h, m, s)}$task\n")
                    }

                    val totalMinutes = totalMillis / 60000
                    val hSum = totalMinutes / 60
                    val mSum = totalMinutes % 60
                    val totalString = "TOTAL TIME: ${String.format("%02dh:%02dm", hSum, mSum)}\n\n"

                    AlertDialog.Builder(requireContext())
                        .setTitle("History for $date")
                        .setMessage(totalString + sb.toString())
                        .setPositiveButton("OK", null)
                        .show()
                } else if (events.isNotEmpty()) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("History for $date")
                        .setMessage(sb.toString())
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    Toast.makeText(context, "No history for $date", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
