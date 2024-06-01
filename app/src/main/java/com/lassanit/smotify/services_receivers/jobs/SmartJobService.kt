package com.lassanit.smotify.services_receivers.jobs

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.lassanit.smotify.bases.CloudBase
import com.lassanit.smotify.handlers.ServiceManager


@SuppressLint("SpecifyJobSchedulerIdRange")
class SmartJobService : JobService() {

    companion object {
        private const val ID_PERIODIC=11
        private const val ID_BOOT=12

        private const val MINUTE=15

        fun getBoot(context: Context): JobInfo {
            val componentName = ComponentName(context, SmartJobService::class.java)
            return JobInfo.Builder(ID_BOOT, componentName)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE) // Specify network conditions
                .setRequiresCharging(false) // True if the job requires the device to be charging
                //.setOverrideDeadline(120_000) // 2 minutes
                .setPeriodic(900_000) // 15 minutes
                .build()
        }
        fun getPeriodic(context: Context): JobInfo {
            val componentName = ComponentName(context, SmartJobService::class.java)
            return JobInfo.Builder(ID_PERIODIC, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE) // Specify network conditions
                .setRequiresCharging(false) // True if the job requires the device to be charging
               // .setOverrideDeadline(120_000) // 2 minutes
                .setPeriodic(MINUTE * 60_000L) // 15 minutes
                .build()
        }
    }

    override fun onStartJob(p0: JobParameters?): Boolean {
        return runCatching {
            Log.d("SmartJobService", " ${p0.toString()}")
            ServiceManager.ensure(applicationContext)
            Firebase.auth.currentUser?.uid?.let { CloudBase.Store(applicationContext, it).requestWriteLink() }
            jobFinished(p0, false)
            true
        }.onFailure {
            it.printStackTrace()
            jobFinished(p0, true)
        }.getOrElse { false }
    }
    override fun onStopJob(p0: JobParameters?): Boolean {
        return false
    }
}