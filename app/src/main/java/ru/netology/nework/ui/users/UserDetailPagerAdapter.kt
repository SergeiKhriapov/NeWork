package ru.netology.nework.ui.users

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class UserDetailPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val wallPostsFragment: WallPostsFragment,
    private val jobsFragment: JobsFragment
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> wallPostsFragment
            1 -> jobsFragment
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}