package com.lassanit.smotify.popups

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.lassanit.smotify.handlers.ServiceManager
import com.lassanit.smotify.services_receivers.SmartPhoneService

class AppResultPermissionDialog(
    private val activity: AppCompatActivity,
    private val type: Type
) {
    companion object {
        fun makeNoti(activity: AppCompatActivity): AppResultPermissionDialog {
            return AppResultPermissionDialog(activity, Type.NOTI)
        }

        fun makeBattery(activity: AppCompatActivity): AppResultPermissionDialog {
            return AppResultPermissionDialog(activity, Type.BATTERY)
        }
    }

    private var onSuccess: Runnable? = null
    private var onFailure: Runnable? = null


    private var resultLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (type) {
                Type.NOTI -> {
                    if (ServiceManager.isNLServiceEnabled(activity)) {
                        onSuccess?.run()
                    } else {
                        onFailure?.run()
                    }
                }

                Type.BATTERY -> {
                    if (ServiceManager.isIgnoringBatteryOptimizations(activity)) {
                        onSuccess?.run()
                    } else {
                        onFailure?.run()
                    }
                }
            }
        }

    enum class Type {
        NOTI, BATTERY
    }

    private lateinit var popup: AppPermissionDialog

    private fun getIntentForNotificationAccess(
        packageName: String,
        notificationAccessServiceClassName: String
    ): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(packageName, notificationAccessServiceClassName).flattenToString()
                )
        }
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        val value = "$packageName/$notificationAccessServiceClassName"
        val key = ":settings:fragment_args_key"
        intent.putExtra(key, value)
        intent.putExtra(":settings:show_fragment_args", Bundle().also { it.putString(key, value) })
        return intent
    }

    private fun getNotificationListenerServiceIntent(): Intent {
        return getIntentForNotificationAccess(
            activity.applicationInfo.packageName,
            SmartPhoneService::class.java.name
        )
    }

    private fun getBatteryOptimizationIntent(): Intent {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        runCatching {
            intent.data = Uri.parse("package:" + activity.applicationInfo.packageName)
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            //    intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.applicationInfo.packageName)
        }.onFailure { it.printStackTrace() }
        return intent
    }

    fun create(onSuccess: Runnable? = null, onFailure: Runnable? = null) {
        runCatching {
            this.onSuccess = onSuccess
            this.onFailure = onFailure
            if (!::popup.isInitialized) {
                popup = AppDialog.needPermission.ask(
                    activity,
                    "Permission Required",
                    buildString {
                        append("")
                        when (type) {
                            Type.NOTI -> {
                                append("Smart Notify Required Notifications access \n")
                            }

                            Type.BATTERY -> {
                                append("Smart Notify Better performance require to Ignore Battery Optimization")
                            }
                        }
                    })
                popup.set({
                    when (type) {
                        Type.NOTI -> {
                            runCatching { resultLauncher.launch(getNotificationListenerServiceIntent()) }
                                .onFailure { resultLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                        }

                        Type.BATTERY -> {
                            runCatching { resultLauncher.launch(getBatteryOptimizationIntent()) }
                                .onFailure { resultLauncher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                        }
                    }
                }, { onFailure?.run() })
            }
        }
    }

    fun show() {
        runCatching { popup.show() }
    }

    fun hide() {
        runCatching { popup.hide() }
    }

    fun dismiss() {
        runCatching { popup.dismiss() }
    }
}