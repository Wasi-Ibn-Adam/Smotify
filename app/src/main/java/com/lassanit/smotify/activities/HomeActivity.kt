package com.lassanit.smotify.activities


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.lassanit.firekit.R.anim
import com.lassanit.smotify.R
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.adapters.AdapterActivityHome
import com.lassanit.smotify.handlers.HomeNavigationHandler
import com.lassanit.smotify.handlers.HomeNavigationImp
import com.lassanit.smotify.handlers.ServiceManager
import com.lassanit.smotify.handlers.ToolbarHandler
import com.lassanit.smotify.handlers.ZoomOutPageTransformer
import com.lassanit.smotify.interfaces.ExternalImps
import com.lassanit.smotify.interfaces.InternalImps
import com.lassanit.smotify.popups.AppDialog


class HomeActivity : AppCompatActivity(),
    HomeNavigationImp,
    InternalImps.ToolBarImp,
    InternalImps.HomeNavExtImp,
    ExternalImps.ServiceManagerImp {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: AdapterActivityHome
    private val popup = AppDialog.needPermission.resultPermissions.makeNoti(this)
    private lateinit var navHandler: HomeNavigationHandler
    private lateinit var toolbarHandler: ToolbarHandler

    companion object {
        private var isNew: Boolean = false
        fun getInstance(context: Context, newUser: Boolean): Intent {
            isNew = newUser
            return Intent(context, HomeActivity::class.java)
                .putExtra("info", newUser)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        fun getInstanceForService(context: Context): Intent {
            return Intent(context, HomeActivity::class.java).putExtra("info", false)
        }

        private var fragImp: InternalImps.HomeNavMenuFragImp? = null
        private var imp: HomeNavigationImp? = null

        fun getFragImp(): HomeNavigationImp? {
            return imp
        }

        fun connect(imp: InternalImps.HomeNavMenuFragImp) {
            fragImp = imp
        }

        fun disconnect() {
            fragImp = null
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(com.lassanit.firekit.R.style.Theme_FireKit_APP)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        Companion.imp = this
        viewPager = findViewById(R.id.app_viewPager)
        viewPager.setPageTransformer(ZoomOutPageTransformer())
        adapter = AdapterActivityHome(this)
        viewPager.adapter = adapter

        toolbarHandler = ToolbarHandler.get(this, this, isUser = false)
        toolbarHandler.setMenu()
        navHandler = HomeNavigationHandler(this, this)

        handleToolbar()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                navHandler.tabItemClick(position)
            }
        })
        popup.create()

        if (intent.getBooleanExtra("info", true)) {
            AppDialog.needDisplay.makeWelcome(this)
                .onDismiss { intent.removeExtra("info");howToUse() }
                .show()
        } else {
            howToUse()
        }
        onBackPressedDispatcher.addCallback(
            this /* lifecycle owner */, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewPager.currentItem == HomeNavigationHandler.DEFAULT_SELECTION)
                        finish()
                    else {
                        onNavigationPosition(HomeNavigationHandler.DEFAULT_SELECTION)
                    }
                }
            })
    }

    private fun howToUse() {
        //AppDialog.needDisplay.makeWelcome(this).show()
        //if (isNew)
        //AppDialog.needDisplay.makeHowToUse(this).onDismiss { isNew = false }.show()
    }

    private fun handleToolbar() {
        toolbarHandler.setToggle(navHandler.getPosition())
        toolbarHandler.setTitle(navHandler.getTitle())
        toolbarHandler.setSubTitle(navHandler.getSubTitle())
    }

    override fun onStart() {
        super.onStart()
        Companion.imp = this
    }

    override fun onResume() {
        super.onResume()
        navHandler.checkRefreshNeeded(HomeActivity::class.java.name)
        ServiceManager.connect(this)
    }

    override fun onNavigationPosition(pos: Int) {
        handleToolbar()
        viewPager.setCurrentItem(pos, true)
    }

    override fun onProfile() {
        runCatching {
            if (SmartNotify.isUser()) {
                startActivity(ProfileActivity.getInstance(this))
            } else {
                val alert = AppDialog.needPermission.ask(
                    this,
                    "Profile Access",
                    buildString {
                        append("Guests are not allowed to make a Profile, if you want to access, ")
                        append("Profile and other features(Secret Notifications, Backup/Restore, ")
                        append("Cloud Services, No ads, etc.)  Join us as A User.").appendLine()
                        append("Your notifications remain secure when you switch to User mode, ")
                        append("and you can still access them. However, if you switch back to Guest ")
                        append("mode, it's like starting fresh, and your previous notifications won't be retained.")
                    },
                    "Join Us"
                )
                alert.set({ SmartNotify.onLogout(this) }, null)
                alert.show()
            }
        }
    }

    override fun onItemCountChange(count: Int) {
        runCatching { navHandler.setNotificationCount(count) }
    }

    override fun onMenuExtend(view: View) {
        AppDialog.needMenu.makeMenuExtension(this, view).show(navHandler.getPosition(), this)
    }

    override fun onNavigationAllRead() {
        fragImp?.onNavigationAllRead()
    }

    override fun onNavigationVisibilityToggle() {
        handleToolbar()
        fragImp?.onNavigationVisibilityToggle()
    }

    override fun onSettings() {
        startActivity(SettingActivity.getInstance(this))
        overridePendingTransition(anim.show, anim.hide)
    }

    override fun serviceState(active: Boolean) {
        if (active.not()) {
            popup.show()
        } else {
            popup.hide()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        popup.dismiss()
    }

}