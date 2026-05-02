package com.example.ideastracker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.ideastracker.databinding.ActivityMainBinding
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

        viewModel.isAsc.observe(this) { isAsc ->
            binding.btnSort.text = if (isAsc == 1) getString(R.string.sort_oldest) else getString(R.string.sort_newest)
        }

        binding.btnSort.setOnClickListener {
            viewModel.toggleSort()
        }

        binding.fabAdd.setOnClickListener {
            showAddIdeaDialog()
        }
    }

    private fun showAddIdeaDialog() {
        val dialog = AddIdeaDialog()
        dialog.show(supportFragmentManager, "AddIdeaDialog")
    }
}
