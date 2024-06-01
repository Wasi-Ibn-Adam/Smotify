package com.lassanit.smotify.handlers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import com.lassanit.smotify.R
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.activities.HomeActivity
import com.lassanit.smotify.activities.SplashActivity


class SmartMessageManager(private val context: Context) {
    private val notificationManager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "notification_listener_channel"
    }

    private val list = ArrayList<String>()

    fun getForegroundNotification(): Notification {
        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }
        builder.setCustomContentView(customView()).setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    1,
                    if (SmartNotify.isLoggedIn())
                        HomeActivity.getInstanceForService(context)
                    else
                        SplashActivity.getInstance(context),
                    PendingIntent.FLAG_MUTABLE
                )
            )
            .setSmallIcon(com.lassanit.firekit.R.drawable.firekit_splash_logo)
        return builder.build()
    }

    fun addApp(pkg: String) {
        if (list.contains(pkg)) {
            list.remove(pkg)
        }
        list.add(0, pkg)
    }

    fun removeApp(pkg: String) {
        if (list.contains(pkg)) {
            list.remove(pkg)
        }
    }

    fun updateForegroundNotification() {
        runCatching {
            val noti = getForegroundNotification()
            notificationManager.notify(FOREGROUND_NOTIFICATION_ID, noti)
        }
    }

    fun createNotificationChannel() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Display",
                    NotificationManager.IMPORTANCE_LOW
                )
                val manager: NotificationManager =
                    context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun customView(): RemoteViews {
        val customNotificationView =
            RemoteViews(context.packageName, R.layout.container_custom_notification)

        customNotificationView.setImageViewResource(
            R.id.app_icon,
            com.lassanit.firekit.R.drawable.firekit_splash_logo
        )
        runCatching {
            customNotificationView.removeAllViews(R.id.notification_list)
            for (pkg in list.take(5)) {
                customNotificationView.addView(R.id.notification_list, getView(pkg))
            }
            if (list.size > 5) {
                customNotificationView.addView(R.id.notification_list, getViewSum(list.size - 5))
            }
        }
        return customNotificationView
    }

    private fun getView(pkg: String): RemoteViews? {
        runCatching {
            val appView =
                RemoteViews(context.packageName, R.layout.handler_msg_manager_notification_img)
            val icon = context.packageManager.getApplicationIcon(pkg).toBitmap()
            appView.setImageViewBitmap(R.id.notification_icon, icon)
            return appView
        }
        return null
    }

    private fun getViewSum(num: Int): RemoteViews? {
        runCatching {
            val appView =
                RemoteViews(context.packageName, R.layout.handler_msg_manager_notification_txt)
            appView.setTextViewText(R.id.notification_text, "+".plus(num))
            return appView
        }
        return null
    }

}