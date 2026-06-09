package com.jules.ideastracker

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarDialog : DialogFragment() {

    private var currentCalendar: Calendar = Calendar.getInstance()
    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private lateinit var calendarGrid: GridLayout
    private lateinit var tvMonthTitle: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(16, 16, 16, 16)
        }

        // Header: Arrows + Month Title
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val btnPrev = ImageButton(requireContext()).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            background = ContextCompat.getDrawable(context, android.R.drawable.screen_background_light_transparent)
            setOnClickListener {
                currentCalendar.add(Calendar.MONTH, -1)
                updateCalendar()
            }
        }

        tvMonthTitle = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            textSize = 18f
            setPadding(0, 0, 0, 0)
        }

        val btnNext = ImageButton(requireContext()).apply {
            setImageResource(android.R.drawable.ic_media_next)
            background = ContextCompat.getDrawable(context, android.R.drawable.screen_background_light_transparent)
            setOnClickListener {
                currentCalendar.add(Calendar.MONTH, 1)
                updateCalendar()
            }
        }

        header.addView(btnPrev)
        header.addView(tvMonthTitle)
        header.addView(btnNext)
        root.addView(header)

        // Day labels (Sun, Mon, etc.)
        val daysHeader = GridLayout(requireContext()).apply {
            columnCount = 7
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val dayNames = arrayOf("S", "M", "T", "W", "T", "F", "S")
        for (day in dayNames) {
            val tv = TextView(requireContext()).apply {
                text = day
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            daysHeader.addView(tv)
        }
        root.addView(daysHeader)

        calendarGrid = GridLayout(requireContext()).apply {
            columnCount = 7
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        root.addView(calendarGrid)

        updateCalendar()

        return root
    }

    private fun updateCalendar() {
        calendarGrid.removeAllViews()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonthTitle.text = sdf.format(currentCalendar.time)

        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Fetch events for this month
        val targetMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            val events = db.calendarEventDao().getAllEventsSync()
            val eventDates = events.filter { it.date.startsWith(targetMonthPrefix) }.map { it.date }.toSet()

            withContext(Dispatchers.Main) {
                // Empty cells before first day
                for (i in 0 until firstDayOfWeek) {
                    calendarGrid.addView(View(requireContext()).apply {
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = 80 // Adjust as needed
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        }
                    })
                }

                // Days of the month
                for (day in 1..daysInMonth) {
                    val dayCal = cal.clone() as Calendar
                    dayCal.set(Calendar.DAY_OF_MONTH, day)
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayCal.time)
                    val hasEvent = eventDates.contains(dateStr)

                    val dayView = FrameLayout(requireContext()).apply {
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = 100
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        }

                        if (hasEvent) {
                            val circle = ContextCompat.getDrawable(context, R.drawable.circle_outline)
                            background = circle
                        }

                        val tv = TextView(context).apply {
                            text = day.toString()
                            gravity = Gravity.CENTER
                            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                        }
                        addView(tv)

                        setOnClickListener {
                            onDayClick(dateStr)
                        }
                    }
                    calendarGrid.addView(dayView)
                }
            }
        }
    }

    private fun onDayClick(date: String) {
        selectedDate = date
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
                                withContext(Dispatchers.Main) {
                                    updateCalendar()
                                }
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
