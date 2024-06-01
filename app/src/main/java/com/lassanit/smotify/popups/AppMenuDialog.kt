package com.lassanit.smotify.popups

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.iterator
import androidx.fragment.app.FragmentActivity
import com.lassanit.smotify.R
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.CloudBaseHelper
import com.lassanit.smotify.bases.DisplayBaseHelper
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.display.adapter.AppHeaderActions
import com.lassanit.smotify.display.adapter.AppMessageActions
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.display.data.AppData
import com.lassanit.smotify.display.data.AppHeader
import com.lassanit.smotify.display.data.AppMessage
import com.lassanit.smotify.display.data.SmartMedia
import com.lassanit.smotify.handlers.HomeNavigationHandler
import com.lassanit.smotify.handlers.LockManager
import com.lassanit.smotify.interfaces.InternalImps
import com.lassanit.smotify.services_receivers.SmartPhoneService
import java.util.Date


class AppMenuDialog(
    private val activity: FragmentActivity,
    view: View? = null,
    type: Int
) {
    private object Type {
        const val T_HEADER = 0
        const val T_MESSAGE = 1
        const val T_MENU_EXTEND = 2
        const val T_APPLICATION = 3
    }

    companion object {
        fun make(
            activity: FragmentActivity,
            view: View? = null,
            actions: AppHeaderActions,
            header: AppHeader,
            isSingle: Boolean = false,
            isCloud: Boolean = false
        ): AppMenuDialog {
            return AppMenuDialog(activity, view, Type.T_HEADER).set(
                actions,
                header,
                isSingle,
                isCloud
            )
        }

        fun make(
            activity: FragmentActivity,
            view: View? = null,
            actions: AppMessageActions,
            msg: AppMessage,
            media: SmartMedia?,
            isPhone: Boolean
        ): AppMenuDialog {
            return AppMenuDialog(activity, view, Type.T_MESSAGE).set(actions, msg, media, isPhone)
        }

        fun makeMenuExtension(
            activity: FragmentActivity, view: View
        ): AppMenuDialog {
            return AppMenuDialog(activity, view, Type.T_MENU_EXTEND)
        }

        fun makeApplication(
            activity: FragmentActivity, view: View
        ): AppMenuDialog {
            return AppMenuDialog(activity, view, Type.T_APPLICATION)
        }
    }

    private val popupMenu = PopupMenu(
        activity,
        view ?: activity.findViewById(R.id.adView),
        Gravity.BOTTOM,
        0,
        R.style.Theme_FireKit_DIALOG_MENU_BOTTOM
    )

    private var appHeader: AppHeader? = null
    private var appHeaderActions: AppHeaderActions? = null

    private var appMessage: AppMessage? = null
    private var appMessageActions: AppMessageActions? = null
    private var appMessageMedia: SmartMedia? = null


    private var appData: AppData? = null
    private var appDataImp: InternalImps.HomeAppItemImp? = null

    private var imp: InternalImps.HomeNavExtImp? = null

    private fun getIntent(pkg: String): Intent? {
        return runCatching {
            val pm: PackageManager = activity.packageManager
            return pm.getLaunchIntentForPackage(pkg)
        }.getOrElse { null }
    }

    init {
        when (type) {
            Type.T_HEADER -> {
                popupMenu.inflate(R.menu.app_header_menu)
            }

            Type.T_MESSAGE -> {
                popupMenu.inflate(R.menu.app_message_menu)
            }

            Type.T_MENU_EXTEND -> {
                popupMenu.inflate(R.menu.app_nav_ext_menu)
            }

            Type.T_APPLICATION -> {
                popupMenu.inflate(R.menu.app_icon_menu)
            }

            else -> {
                throw Exception("Type is Invalid for ItemMenuHandler")
            }
        }
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_open -> {
                    appHeader?.let { it1 ->
                        runCatching {
                            val intent = getIntent(it1.pkg)
                            if (intent != null)
                                activity.startActivity(intent)
                        }.onFailure { it2 -> it2.printStackTrace() }
                    }
                }

                R.id.item_select -> {
                    appHeader?.let { it1 -> appHeaderActions?.selectHeader(it1, true) }
                }

                R.id.item_read -> appHeader?.let { it1 -> appHeaderActions?.readHeader(it1) }
                R.id.item_show, R.id.item_hide -> {
                    if (SmartNotify.isGuest()) {
                        Toast.makeText(
                            activity, "Guest can not Hide Notifications", Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        LockManager(activity, {
                            appHeader?.let { it1 -> appHeaderActions?.visibilityHeader(it1) }
                        }, null).show()
                    }
                }

                R.id.item_details -> appHeader?.let { it1 ->
                    AppDialog.needDisplay.make(activity, it1).show()
                }

                R.id.item_delete -> {
                    runCatching {
                        val pop = AppDialog.needPermission.ask(
                            activity,
                            "Delete '${(appHeader?.pkgName) ?: "App"}' Data",
                            "All Notifications of '${(appHeader?.pkgName) ?: "this Application"}' will be deleted...",
                            "Delete All"
                        )
                        pop.set({
                            LockManager(activity, {
                                appHeader?.let { it1 -> appHeaderActions?.deleteHeader(it1) }
                            }, null).show()
                        }, null)
                        pop.show()
                    }
                }

                R.id.item_msg_details -> appMessage?.let { it1 ->
                    AppDialog.needDisplay.make(activity, it1, appMessageMedia) { sharingView ->
                        Resource.shareViewAsImage(
                            sharingView, System.currentTimeMillis().toString()
                        ) { sharingIntent ->
                            if (sharingIntent != null)
                                activity.startActivity(
                                Intent.createChooser(sharingIntent, "Share as Image")
                            )
                        }
                    }.show()
                }

                R.id.item_msg_link -> {
                    appMessage?.let { a ->
                        runCatching {
                            SmartPhoneService.getAction(a.id)?.send()
                        }.onFailure { e: Throwable -> e.printStackTrace() }
                    }
                }

                R.id.item_msg_media_img, R.id.item_msg_media_audio -> {
                    appMessage?.let { a ->
                        if (a.hasMedia) {
                            appMessageMedia?.let { media ->
                                AppDialog.needDisplay.make(activity, media)?.show()
                            }
                        }
                    }
                }

                R.id.item_msg_delete -> {
                    runCatching {
                        val pop = AppDialog.needPermission.ask(
                            activity,
                            "Delete '${(appMessage?.title) ?: " Notification"}'",
                            "Notification will be deleted...",
                            "Confirm"
                        )
                        pop.set({
                            LockManager(activity, {
                                appMessage?.let { it1 ->
                                    appMessageActions?.deleteMessage(it1)
                                }
                            }, null).show()
                        }, null)
                        pop.show()
                    }
                }

                R.id.nav_all_read -> imp?.onNavigationAllRead()

                R.id.nav_app_show -> {
                    if (SmartNotify.isGuest()) {
                        Toast.makeText(
                            activity,
                            "Guest can not open Secret Notifications",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        LockManager(activity, {
                            SharedBase.Visibility.Phone.toggleNormalVsSecretState()
                            imp?.onNavigationVisibilityToggle()
                        }, null).show()
                    }
                }

                R.id.nav_app_hide -> {
                    SharedBase.Visibility.Phone.toggleNormalVsSecretState()
                    imp?.onNavigationVisibilityToggle()
                }

                R.id.nav_cloud_show -> {
                    LockManager(activity, {
                        SharedBase.Visibility.toggleCloudVsPhoneState()
                        imp?.onNavigationVisibilityToggle()
                    }, null).show()
                }

                R.id.nav_phone_show -> {
                    SharedBase.Visibility.toggleCloudVsPhoneState()
                    imp?.onNavigationVisibilityToggle()
                }

                R.id.nav_settings -> imp?.onSettings()

                R.id.item_app_off -> {
                    appDataImp?.onItem(false)
                }

                R.id.item_app_on -> {
                    appDataImp?.onItem(true)
                }

                R.id.item_app_copy -> {
                    appData?.let {
                        val string = buildString {
                            append("App Name: ").append(it.app.name).appendLine()
                            append("App Id: ").append(it.app.pkg).appendLine()
                            append("App Version: ").append(it.version).appendLine()
                            append("App Install Time: ").append(Date(it.install).toString())
                                .appendLine()
                            append("App Last Update Time: ").append(Date(it.update).toString())
                                .appendLine()
                            append("App Store Link: ").append(Resource.storeLink(it.app.pkg))
                                .appendLine()
                        }
                        Resource.copyToClipBoard(activity, string)
                    }
                }

                R.id.item_app_share -> {
                    appData?.let {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        val textToShare = buildString {
                            append("App Name: ").append(it.app.name).appendLine()
                            append("App Id: ").append(it.app.pkg).appendLine()
                            append("App Version: ").append(it.version).appendLine()
                            append("App Install Time: ").append(Date(it.install).toString())
                                .appendLine()
                            append("App Last Update Time: ").append(Date(it.update).toString())
                                .appendLine()
                            append("Store Link: ").append(Resource.storeLink(it.app.pkg))
                                .appendLine()
                            append("Also Install and Register with ${activity.getString(R.string.app_name)} ").append(
                                Resource.storeLink(activity.packageName)
                            )
                                .appendLine()
                        }
                        shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare)
                        activity.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                }

                R.id.item_app_details -> {
                    appData?.let { data -> AppDialog.needDisplay.make(activity, data).show() }
                }
            }
            true
        }
    }

    private fun set(
        actions: AppHeaderActions,
        header: AppHeader,
        isSingle: Boolean,
        isCloud: Boolean
    ): AppMenuDialog {
        appHeaderActions = actions
        appHeader = header
        for (item in popupMenu.menu.iterator()) {
            menuIconTint(item.icon)
            when (item.itemId) {
                R.id.item_open -> {
                    item.isVisible = (appHeader?.pkg?.let { getIntent(it) } != null)
                }

                R.id.item_select -> {
                    item.isVisible = isSingle.not()
                }

                R.id.item_show -> {
                    item.isVisible = (header.visibility == SharedBase.Visibility.SECRET)
                        .and(isCloud.not())
                        .and(SmartNotify.isUser())
                }

                R.id.item_hide -> {
                    item.isVisible = (header.visibility == SharedBase.Visibility.NORMAL)
                        .and(isCloud.not())
                        .and(SmartNotify.isUser())
                }

                R.id.item_read -> {
                    item.isVisible = header.unread > 0
                }
            }
        }
        return this
    }

    private fun set(
        actions: AppMessageActions,
        msg: AppMessage,
        media: SmartMedia?,
        isPhone: Boolean
    ): AppMenuDialog {
        appMessageActions = actions
        appMessage = msg
        appMessageMedia = media
        for (item in popupMenu.menu.iterator()) {
            menuIconTint(item.icon)
            when (item.itemId) {
                R.id.item_msg_media_img -> {
                    item.isVisible = appMessageMedia?.isImage(activity) == true
                }

                R.id.item_msg_media_audio -> {
                    item.isVisible = appMessageMedia?.isAudio(activity) == true
                }

                R.id.item_msg_link -> runCatching {
                    item.isVisible =
                        (SmartPhoneService.getAction(appMessage!!.id) != null).and(isPhone)
                }
            }
        }
        return this
    }

    private fun handleMenu(position: Int) {
        val isNoti = position == HomeNavigationHandler.POS_NOTI

        popupMenu.setForceShowIcon(true)
        popupMenu.gravity = Gravity.END
        for (item in popupMenu.menu.iterator()) {
            //        menuIconTint(item.icon)
            when (item.itemId) {
                R.id.nav_all_read -> {
                    val count = runCatching {
                        DisplayBaseHelper.get(activity).getUnreadMessagesCount()
                    }.getOrElse { 0L }
                    item.isVisible = isNoti.and(count > 0)
                }

                R.id.nav_app_show -> {
                    item.isVisible = isNoti
                        .and(SharedBase.Visibility.isPhone())
                        .and(SharedBase.Visibility.Phone.isNormalNotifications())
                        .and(SmartNotify.isUser())
                }

                R.id.nav_app_hide -> {
                    item.isVisible = isNoti
                        .and(SharedBase.Visibility.isPhone())
                        .and(SharedBase.Visibility.Phone.isSecretNotifications())
                        .and(SmartNotify.isUser())
                }

                R.id.nav_phone_show -> {
                    item.isVisible = isNoti
                        .and(SharedBase.Visibility.isCloud())
                        .and(SmartNotify.isUser())
                }

                R.id.nav_cloud_show -> {
                    item.isVisible = isNoti
                        .and(SharedBase.Visibility.isPhone())
                        .and(SharedBase.Visibility.Phone.isNormalNotifications())
                        .and(
                            SharedBase.CloudService.isActive()
                                .or(CloudBaseHelper.getViewer(activity).hasMessages())
                        )
                        .and(SmartNotify.isUser())
                }
            }
        }
    }

    private fun handleAppData(active: Boolean) {
        for (item in popupMenu.menu.iterator()) {
            menuIconTint(item.icon)
            when (item.itemId) {
                R.id.item_app_off -> item.isVisible = active
                R.id.item_app_on -> item.isVisible = active.not()
            }
        }
    }

    private fun menuIconTint(icon: Drawable?) {
        if (icon != null) {
            DrawableCompat.setTint(
                icon, ContextCompat.getColor(
                    activity, com.lassanit.firekit.R.color.fireKit_opposite_theme
                )
            )
        }
    }

    fun show() {
        popupMenu.show()
    }

    fun show(position: Int, imp: InternalImps.HomeNavExtImp) {
        this.imp = imp
        handleMenu(position)
        popupMenu.show()
        popupMenu.setOnDismissListener { }
    }

    fun show(data: App, imp: InternalImps.HomeAppItemImp) {
        this.appDataImp = imp
        this.appData = Resource.getAppDetails(activity, data)
        handleAppData(appData!!.active)
        popupMenu.show()
    }

}