package com.lassanit.smotify.handlers

import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.lassanit.smotify.R
import com.lassanit.smotify.activities.ProfileActivity
import com.lassanit.smotify.bases.SharedBase
import de.hdodenhof.circleimageview.CircleImageView

interface HomeNavigationImp:NavigationBaseImp {
    fun onProfile()
    fun onItemCountChange(count: Int)
}

class HomeNavigationHandler(activity: AppCompatActivity, imp: HomeNavigationImp?) :
    NavigationHandler(activity, imp, R.id.handler_nav_bar, R.id.btm_nav) {

    private val userImg: CircleImageView = view.findViewById(R.id.btm_profile)
    private val apps: TabLayout.Tab = tabLayout.newTab()
        .setIcon(R.drawable.base_app_unselected)
    private val notification: TabLayout.Tab = tabLayout.newTab()
        .setIcon(R.drawable.base_noti_unselected)

    companion object {
        const val TOTAL_POS = 2
        const val POS_NOTI = 0
        const val POS_APPS = 1
        const val DEFAULT_SELECTION = POS_NOTI
    }

    override fun getDefaultItem(): Int {
        return DEFAULT_SELECTION
    }

    override fun onNavigationTabUnselected(tab: TabLayout.Tab?) {
        when (tab) {
            apps -> {
                tab.setIcon(R.drawable.base_app_unselected)
            }

            notification -> {
                tab.setIcon(R.drawable.base_noti_unselected)
            }

            else -> {}
        }

    }

    override fun onNavigationTabSelected(tab: TabLayout.Tab?) {
        when (tab) {
            apps -> {
                tab.setIcon(R.drawable.base_app_selected)
            }

            notification -> {
                tab.setIcon(R.drawable.base_noti_selected)
            }

            else -> {}
        }
    }

    override fun getTabList(): List<TabLayout.Tab> {
        return listOf(notification, apps)
    }

    init {
        setup()
        notification.apply { orCreateBadge.isVisible = false }
        setProfile()
    }

    fun getTitle(): String {
        return when (getPosition()) {
            POS_APPS -> "Apps"
            POS_NOTI -> "Notifications"
            else -> ""
        }
    }

    fun getSubTitle(): String {
        return when (getPosition()) {
            POS_APPS -> "Choose notification apps."
            POS_NOTI -> {
                if (SharedBase.Visibility.isPhone()) {
                    if (SharedBase.Visibility.Phone.isNormalNotifications()) {
                        " "
                    } else
                        "Secret Notifications List"
                } else
                    "Cloud Notifications List"
            }

            else -> " "
        }
    }

    fun setNotificationCount(count: Int) {
        runCatching {
            notification.apply { orCreateBadge.isVisible = (count > 0); badge?.number = count }
        }
    }

    fun checkRefreshNeeded(className: String) {
        runCatching {
            if (ProfileActivity.isUserUpdated(className)) {
                setProfile()
                ProfileActivity.updateUsed(className)
            }
        }
    }

    private fun setProfile() {
        runCatching {
            Glide.with(activity).asDrawable()
                .circleCrop()
                .load(Firebase.auth.currentUser?.photoUrl ?: R.drawable.base_user)
                .into(userImg)
        }
    }
}
