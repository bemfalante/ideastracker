package com.jules.ideastracker

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class IdeasPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> IdeasFragment.newInstance(IdeaStatus.DONE)
            1 -> IdeasFragment.newInstance(IdeaStatus.ONGOING)
            2 -> IdeasFragment.newInstance(IdeaStatus.FUTURE)
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
