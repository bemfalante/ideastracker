package com.jules.ideastracker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.jules.ideastracker.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: IdeaViewModel by viewModels {
        IdeaViewModelFactory(AppDatabase.getDatabase(this).ideaDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = IdeasPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.title_done)
                1 -> getString(R.string.title_ongoing)
                2 -> getString(R.string.title_future)
                else -> null
            }
        }.attach()

        binding.viewPager.setCurrentItem(1, false) // Start at Ongoing
        updateNavHeader(1)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateNavHeader(position)
            }
        })

        // Navigation through header clicks (Rule 5: pressing captions or arrows)
        binding.tvLeftNav.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current > 0) binding.viewPager.currentItem = current - 1
        }
        binding.tvRightNav.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < 2) binding.viewPager.currentItem = current + 1
        }
        // Center text also clickable to confirm "active" status or do nothing,
        // but user said "navigate over panels also pressing the captions' texts".
        // I'll make the whole header area more intuitive.

        viewModel.isAsc.observe(this) { isAsc ->
            binding.btnSort.text = if (isAsc == 1) getString(R.string.sort_oldest) else getString(R.string.sort_newest)
        }

        binding.btnSort.setOnClickListener {
            viewModel.toggleSort()
        }

        binding.btnNotes.setOnClickListener {
            startActivity(android.content.Intent(this, NotesActivity::class.java))
        }

        binding.btnBills.setOnClickListener {
            startActivity(android.content.Intent(this, BillsActivity::class.java))
        }

        binding.fabAdd.setOnClickListener {
            showAddIdeaDialog()
        }

        binding.btnCalendar.setOnClickListener {
            showCalendarDialog()
        }

        // Rule 4: Auto scroll to new idea
        viewModel.lastInsertedId.observe(this) { id ->
            // Move to Future panel (index 2) because new ideas are created as FUTURE
            binding.viewPager.setCurrentItem(2, true)
        }
    }

    private fun updateNavHeader(position: Int) {
        val done = getString(R.string.title_done)
        val ongoing = getString(R.string.title_ongoing)
        val future = getString(R.string.title_future)

        when (position) {
            0 -> { // Done
                binding.tvLeftNav.text = ""
                binding.tvCenterNav.text = done
                binding.tvRightNav.text = "$ongoing →"
            }
            1 -> { // Ongoing
                binding.tvLeftNav.text = "← $done"
                binding.tvCenterNav.text = ongoing
                binding.tvRightNav.text = "$future →"
            }
            2 -> { // Future
                binding.tvLeftNav.text = "← $ongoing"
                binding.tvCenterNav.text = future
                binding.tvRightNav.text = ""
            }
        }
    }

    private fun showAddIdeaDialog() {
        val dialog = AddIdeaDialog.newInstance()
        dialog.show(supportFragmentManager, "AddIdeaDialog")
    }

    private fun showCalendarDialog() {
        val dialog = CalendarDialog()
        dialog.show(supportFragmentManager, "CalendarDialog")
    }
}
