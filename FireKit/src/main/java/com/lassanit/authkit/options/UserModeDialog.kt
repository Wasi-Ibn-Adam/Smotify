package com.lassanit.authkit.options

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.lassanit.firekit.R

class UserModeDialog(context: Context) {

    val dialog = Dialog(context, R.style.Theme_FireKit_APP_DIALOG)

    init {
        dialog.setContentView(R.layout.container_user_consent_guest_mode)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setGravity(Gravity.CENTER)
    }

    fun show() {
        try {
            if (dialog.isShowing.not())
                dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismiss() {
        try {
            dialog.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun set(onGuest: Runnable, onUser: Runnable) {
        dialog.findViewById<View>(R.id.user_consent)
            .setOnClickListener { onUser.run() }
        dialog.findViewById<View>(R.id.guest_consent)
            .setOnClickListener { onGuest.run() }
    }


}