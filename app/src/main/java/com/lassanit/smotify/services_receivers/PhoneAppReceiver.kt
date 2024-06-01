package com.lassanit.smotify.services_receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.lassanit.smotify.bases.SmartBase
import com.lassanit.smotify.interfaces.ExternalImps

class PhoneAppReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context, p1: Intent?) {
        runCatching {
            when (p1?.action) {
                "android.intent.action.PACKAGE_ADDED" -> {
                    p1.data?.schemeSpecificPart?.let {
                        runCatching { SmartBase(p0).editHelper.addApp(it) }
                        actions?.onPhoneAppAdded(it)
                    }
                }

                "android.intent.action.PACKAGE_REMOVED" -> {
                    p1.data?.schemeSpecificPart?.let { actions?.onPhoneAppRemoved(it) }
                }

                "android.intent.action.PACKAGE_REPLACED", "android.intent.action.PACKAGE_CHANGED" -> {
                    p1.data?.schemeSpecificPart?.let {
                        runCatching { SmartBase(p0).editHelper.addApp(it) }
                        actions?.onPhoneAppUpdated(it)
                    }
                }

                else -> {}
            }
        }.onFailure { e: Throwable -> Firebase.crashlytics.recordException(e);e.printStackTrace() }
    }

    companion object {
        private var actions: ExternalImps.AppsImp? = null
        private var has: Boolean = false

        fun connect(actions: ExternalImps.AppsImp) {
            this.actions = actions
            if (has) {
                actions.hasPreviousData()
                has = false
            }
        }

        fun disconnect() {
            this.actions = null
        }

        fun getImp(): ExternalImps.AppsImp? {
            if (actions == null)
                has = true
            return actions
        }
    }
}