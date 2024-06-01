package com.lassanit.smotify.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.lassanit.smotify.fragments.SettingsPreferencesFragment
import com.lassanit.smotify.fragments.SettingsProfileFragment
import com.lassanit.smotify.handlers.SettingsNavigationHandler

class AdapterActivitySettings(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return SettingsNavigationHandler.TOTAL_POS // Number of fragments
    }
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            SettingsNavigationHandler.POS_PROFILE -> {
                SettingsProfileFragment()
            }

            SettingsNavigationHandler.POS_PREFERENCE -> {
                SettingsPreferencesFragment()
            }

            else -> throw IllegalArgumentException("Invalid position: $position, Navigation size is Greater than Adapter")
        }
    }

}