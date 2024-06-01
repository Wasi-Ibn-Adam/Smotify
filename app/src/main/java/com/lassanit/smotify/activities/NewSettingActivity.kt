package com.lassanit.smotify.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.lassanit.smotify.R
import com.lassanit.smotify.adapters.AdapterActivitySettings
import com.lassanit.smotify.handlers.NavigationBaseImp
import com.lassanit.smotify.handlers.SettingsNavigationHandler
import com.lassanit.smotify.handlers.ToolbarHandler
import com.lassanit.smotify.handlers.ZoomOutPageTransformer
import com.lassanit.smotify.interfaces.InternalImps

class NewSettingActivity : AppCompatActivity(), NavigationBaseImp, InternalImps.ToolBarImp{
    companion object {
        fun getInstance(context: Context): Intent {
            return Intent(context, NewSettingActivity::class.java)
        }
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: AdapterActivitySettings
    private lateinit var navHandler: SettingsNavigationHandler
    private lateinit var toolbarHandler: ToolbarHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_new)
        runCatching {
            viewPager = findViewById(R.id.app_viewPager)
            viewPager.setPageTransformer(ZoomOutPageTransformer())
            adapter = AdapterActivitySettings(this)
            viewPager.adapter = adapter

            toolbarHandler = ToolbarHandler.get(this, this, isUser = false)
            toolbarHandler.setMenu()
            navHandler = SettingsNavigationHandler(this,this)

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    navHandler.tabItemClick(position)
                }
            })
        }.onFailure { it.printStackTrace() }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            toolbarHandler.checkRefreshNeeded(NewSettingActivity::class.java.name)
        }.onFailure { it.printStackTrace() }
    }

    private fun handleToolbar() {
        runCatching{
            toolbarHandler.setToggle(navHandler.getPosition())
            toolbarHandler.setTitle(navHandler.getTitle())
            toolbarHandler.setSubTitle(navHandler.getSubTitle())
        }.onFailure { it.printStackTrace() }
    }

    override fun onNavigationPosition(pos: Int) {
        runCatching{
            handleToolbar()
            viewPager.setCurrentItem(pos, true)
        }.onFailure { it.printStackTrace() }
    }

    override fun onMenuExtend(view: View) {

    }
}