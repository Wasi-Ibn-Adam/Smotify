package com.lassanit.smotify.handlers

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.interfaces.ExternalImps
import com.lassanit.smotify.services_receivers.PhoneAppReceiver
import com.lassanit.smotify.services_receivers.SmartPhoneService
import com.lassanit.smotify.services_receivers.jobs.SmartJobService
import com.lassanit.smotify.services_receivers.jobs.SmartReceiver

class ServiceManager {
    object NLService {
        private fun isServiceRunning(context: Context): Boolean {
            return runCatching {
                val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                for (service: ActivityManager.RunningServiceInfo in manager.getRunningServices(Int.MAX_VALUE)) {
                    if (SmartPhoneService::class.java.name == service.service.className) return true
                }
                false
            }.getOrElse { false }
        }

        fun isServiceEnabled(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val componentName = ComponentName(context, SmartPhoneService::class.java)
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.isNotificationListenerAccessGranted(componentName)
            } else NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }

        fun ensure(context: Context, imp: ExternalImps.ServiceManagerImp?) {
            runCatching {
                if (isServiceEnabled(context)) {
                    imp?.serviceState(true)
                    val componentName = ComponentName(context, SmartPhoneService::class.java)
                    val packageManager: PackageManager = context.packageManager
                    val disabled = isServiceRunning(context.applicationContext).not()
                    when (val res = packageManager.getComponentEnabledSetting(componentName)) {
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> {
                            SmartNotify.log(
                                "PackageManager.COMPONENT_ENABLED_STATE_DEFAULT",
                                TAG
                            )
                            if (disabled)
                                packageManager.setComponentEnabledSetting(
                                    componentName,
                                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                    PackageManager.DONT_KILL_APP
                                )
                            else {
                                //
                            }
                        }

                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> {
                            SmartNotify.log(
                                "PackageManager.COMPONENT_ENABLED_STATE_ENABLED",
                                TAG
                            )
                            if (disabled) {
                                packageManager.setComponentEnabledSetting(
                                    componentName,
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    PackageManager.DONT_KILL_APP
                                )
                                packageManager.setComponentEnabledSetting(
                                    componentName,
                                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                    PackageManager.DONT_KILL_APP
                                )

                            } else {
                                //
                            }
                        }

                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> {
                            SmartNotify.log(
                                "PackageManager.COMPONENT_ENABLED_STATE_DISABLED",
                                TAG
                            )
                            //if (disabled)
                            packageManager.setComponentEnabledSetting(
                                componentName,
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                PackageManager.DONT_KILL_APP
                            )
                        }

                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER -> {
                            SmartNotify.log(
                                "PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER",
                                TAG
                            )
                        }

                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> {
                            SmartNotify.log(
                                "PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED",
                                TAG
                            )
                        }

                        else -> {
                            SmartNotify.log("NL-Ensure:else :: $res", TAG)
                        }
                    }
                } else {
                    imp?.serviceState(false)
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    object Scheduler {
        fun ensure(context: Context) {
            scheduleAlarm(context)
            scheduleJobs(context)
        }

        private fun scheduleJobs(context: Context) {
            runCatching {
                val jobScheduler =
                    context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                jobScheduler.schedule(SmartJobService.getBoot(context))
                jobScheduler.schedule(SmartJobService.getPeriodic(context))
            }.onFailure { it.printStackTrace() }
        }

        private fun scheduleAlarm(context: Context) {
            runCatching {
                val intent = Intent(context, SmartReceiver::class.java)
                val alarmManager =
                    context.getSystemService(Application.ALARM_SERVICE) as AlarmManager

                val interval = (1.5 * 60 * 1000).toLong() // 1 minute in milliseconds
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    SystemClock.elapsedRealtime() + interval,
                    interval,
                    PendingIntent.getBroadcast(context, 10, intent, PendingIntent.FLAG_IMMUTABLE)
                )
                alarmManager.setRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + interval,
                    (interval * 1.5).toLong(),
                    PendingIntent.getBroadcast(context, 11, intent, PendingIntent.FLAG_IMMUTABLE)
                )
            }.onFailure { it.printStackTrace() }
        }
    }



    companion object {
        private const val TAG = "-ServiceManager"
        private var imp: ExternalImps.ServiceManagerImp? = null
        fun connect(imp: ExternalImps.ServiceManagerImp? = null) {
            Companion.imp = imp
        }

        fun setup(context: Context) {
            SmartNotify.log("setup(context)", TAG)
            runCatching { setAppPackageListener(context);setAppServiceListener(context) }
            runCatching { Scheduler.ensure(context) }
            runCatching { ensure(context) }
        }

        fun slowSetup(context: Context) {
            SmartNotify.log("slowSetup(context)", TAG)
            Scheduler.ensure(context)
        }


        private fun setAppPackageListener(context: Context) {
            runCatching {
                val intentFilter = IntentFilter()
                intentFilter.addAction("android.intent.action.PACKAGE_ADDED")
                intentFilter.addAction("android.intent.action.PACKAGE_REMOVED")
                intentFilter.addAction("android.intent.action.PACKAGE_REPLACED")
                intentFilter.addDataScheme("package")
                context.registerReceiver(PhoneAppReceiver(), intentFilter)
            }
                .onFailure { it.printStackTrace() }
        }

        private fun setAppServiceListener(context: Context) {
            runCatching {
                val intentFilter = IntentFilter()
                intentFilter.addAction("android.intent.action.BOOT_COMPLETED")
                intentFilter.addAction("android.intent.action.LOCKED_BOOT_COMPLETED")
                intentFilter.addAction("android.intent.action.SCREEN_ON")
                intentFilter.addAction("android.intent.action.SCREEN_OFF")
                intentFilter.addAction("android.intent.action.USER_UNLOCKED")
                context.registerReceiver(PhoneAppReceiver(), intentFilter)
            }.onFailure { it.printStackTrace() }
        }

        fun isNLServiceEnabled(context: Context): Boolean {
            return NLService.isServiceEnabled(context)
        }

        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            return runCatching {
                val manager =
                    context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                manager.isIgnoringBatteryOptimizations(context.applicationContext.packageName)
            }.onFailure { it.printStackTrace() }.getOrElse { false }
        }

        fun ensure(context: Context) {
            SmartNotify.log("ensure(context)", TAG)
            runCatching {
                NLService.ensure(context, imp)
            }
        }
    }
}