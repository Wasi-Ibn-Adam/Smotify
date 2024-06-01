package com.lassanit.smotify.popups

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import com.lassanit.firekit.R

open class AppDialog(context: Context, dimAble: Boolean = true) :
    Dialog(
        context,
        if (dimAble) R.style.Theme_FireKit_APP_DIALOG_DIM else R.style.Theme_FireKit_APP_DIALOG
    ) {
    protected fun initFinalize(
        outsideTouch: Boolean = false,
        fullView: Boolean = false,
        appTheme: Boolean = false
    ) {
        setCanceledOnTouchOutside(outsideTouch)
        window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            if (fullView)
                LinearLayout.LayoutParams.MATCH_PARENT
            else
                LinearLayout.LayoutParams.WRAP_CONTENT
        )
        if (appTheme)
            window?.setBackgroundDrawableResource(R.drawable.firekit_window_background_1)
        window?.setGravity(Gravity.CENTER)
    }

    companion object {
        val needDisplay = AppDataDisplayDialog.Companion
        val needInput = AppInputDialog.Companion
        val needPermission = AppPermissionDialog.Companion
        val needMenu = AppMenuDialog.Companion
    }

}