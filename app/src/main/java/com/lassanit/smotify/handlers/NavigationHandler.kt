package com.lassanit.smotify.handlers

import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout

interface NavigationBaseImp {
    fun onNavigationPosition(pos: Int)
}

abstract class NavigationHandler(
    protected val activity: AppCompatActivity,
    protected val imp: NavigationBaseImp?,
    @IdRes val viewId: Int,
    @IdRes val tabLayoutId: Int,
) {

    protected val view: View = activity.findViewById(viewId)
    protected val tabLayout: TabLayout = view.findViewById(tabLayoutId)

    abstract fun getTabList(): List<TabLayout.Tab>
    abstract fun getDefaultItem(): Int
    abstract fun onNavigationTabUnselected(tab: TabLayout.Tab?)
    abstract fun onNavigationTabSelected(tab: TabLayout.Tab?)

    fun setup() {
        val tabs = getTabList()
        for (tab in tabs) {
            tabLayout.addTab(tab)
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                onNavigationTabSelected(tab)
                imp?.onNavigationPosition(getTabList().indexOf(tab))
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                onNavigationTabUnselected(tab)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
        tabItemClick(getDefaultItem())
        tabLayout.setTabIconTintResource(com.lassanit.firekit.R.color.fireKit_opposite_theme)
    }

    fun tabItemClick(pos: Int) {
        val tab = tabLayout.getTabAt(pos)
        if (tabLayout.selectedTabPosition != pos)
            tab?.select()
        onNavigationTabSelected(tab)
    }

    fun getPosition(): Int {
        return tabLayout.selectedTabPosition
    }

}
