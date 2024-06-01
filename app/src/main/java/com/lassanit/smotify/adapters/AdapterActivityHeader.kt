package com.lassanit.smotify.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.lassanit.smotify.fragments.HomeAppFragment
import com.lassanit.smotify.fragments.HomeNotificationFragment
import com.lassanit.smotify.handlers.HomeNavigationHandler.Companion.POS_APPS
import com.lassanit.smotify.handlers.HomeNavigationHandler.Companion.POS_NOTI
import com.lassanit.smotify.handlers.HomeNavigationHandler.Companion.TOTAL_POS

class AdapterActivityHeader(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return TOTAL_POS // Number of fragments
    }

    override fun createFragment(position: Int): Fragment {

        return when (position) {
            POS_APPS -> {
                HomeAppFragment()
            }

            POS_NOTI -> {
                HomeNotificationFragment()
            }

            else -> throw IllegalArgumentException("Invalid position: $position, Navigation size is Greater than Adapter")
        }
    }

}