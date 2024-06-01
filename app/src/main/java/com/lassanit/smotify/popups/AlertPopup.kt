package com.lassanit.smotify.popups

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

abstract class AlertPopup(protected var activity: AppCompatActivity) {
    protected lateinit var dialog: AlertDialog
    protected var runnable: Runnable? = null
    protected var cancelable: Runnable? = null

    companion object {
        fun display(activity: Activity, view: View): AlertDialog {
            val dialog = AlertDialog.Builder(activity).create()
            dialog.setCanceledOnTouchOutside(true)
            dialog.setView(view)
            return dialog
        }
    }

    protected fun inflate(@LayoutRes res: Int): View {
        return LayoutInflater.from(activity).inflate(res, null)
    }

    fun show() {
        runCatching { if (::dialog.isInitialized && !dialog.isShowing) dialog.show() }
    }

    fun set(
        onPositive: Runnable? = null,
        onNegative: Runnable? = null,
        onDismiss: Runnable? = null
    ) {
        this.runnable = onPositive
        this.cancelable = onNegative
        if (::dialog.isInitialized)
            dialog.setOnDismissListener { onDismiss?.run() }
    }

    fun onPositive(runnable: Runnable) {
        this.runnable = runnable
    }

    fun onNegative(runnable: Runnable) {
        this.cancelable = runnable
    }

    fun onDismiss(runnable: Runnable) {
        if (::dialog.isInitialized)
            dialog.setOnDismissListener { runnable.run() }
    }

    fun hide() {
        if (::dialog.isInitialized && dialog.isShowing)
            dialog.dismiss()
    }

}