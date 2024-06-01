package com.lassanit.smotify.services_receivers.jobs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lassanit.smotify.handlers.ServiceManager

class SmartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, p1: Intent?) {
        if (context == null) return
        Log.d("SmartReceiver", " ${p1?.action}")
        runCatching {
            ServiceManager.ensure(context)
        }
    }
}