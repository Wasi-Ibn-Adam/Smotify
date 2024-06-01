package com.lassanit.smotify.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.lassanit.smotify.fragments.HomeAppFragment
import com.lassanit.smotify.fragments.HomeNotificationFragment
import com.lassanit.smotify.handlers.HomeNavigationHandler

class AdapterActivityHome(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return HomeNavigationHandler.TOTAL_POS // Number of fragments
    }
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            HomeNavigationHandler.POS_APPS -> {
                HomeAppFragment()
            }

            HomeNavigationHandler.POS_NOTI -> {
                HomeNotificationFragment()
            }

            else -> throw IllegalArgumentException("Invalid position: $position, Navigation size is Greater than Adapter")
        }
    }

}