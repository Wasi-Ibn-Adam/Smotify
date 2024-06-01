package com.lassanit.smotify.handlers

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.lassanit.smotify.R

class SettingsNavigationHandler(activity: AppCompatActivity, imp: NavigationBaseImp?) :
    NavigationHandler(activity, imp, R.id.handler_nav_bar, R.id.btm_nav) {

    private val profile: TabLayout.Tab = tabLayout.newTab()
        .setIcon(R.drawable.base_user)

    private val preferences: TabLayout.Tab = tabLayout.newTab()
        .setIcon(R.drawable.base_list_top)

    companion object {
        const val TOTAL_POS = 2
        const val POS_PROFILE = 0
        const val POS_PREFERENCE = 1
        const val DEFAULT_SELECTION = POS_PREFERENCE
    }

    override fun getDefaultItem(): Int {
        return DEFAULT_SELECTION
    }

    override fun onNavigationTabUnselected(tab: TabLayout.Tab?) {
        when (tab) {
            profile -> {
                tab.setIcon(R.drawable.base_app_unselected)
            }

            preferences -> {
                tab.setIcon(R.drawable.base_noti_unselected)
            }

            else -> {}
        }

    }

    override fun onNavigationTabSelected(tab: TabLayout.Tab?) {
        when (tab) {
            profile -> {
                tab.setIcon(R.drawable.base_app_selected)
            }

            preferences -> {
                tab.setIcon(R.drawable.base_noti_selected)
            }

            else -> {}
        }
    }

    override fun getTabList(): List<TabLayout.Tab> {
        return listOf(profile,preferences)
    }

    init {
        setup()
    }

    fun getTitle(): String {
        return when (getPosition()) {
            POS_PROFILE -> "Profile "
            POS_PREFERENCE -> "Preferences "
            else -> ""
        }
    }

    fun getSubTitle(): String {
        return when (getPosition()) {
            else -> " "
        }
    }


}
