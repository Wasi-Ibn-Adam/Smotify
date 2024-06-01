package com.lassanit.extras

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.lassanit.firekit.R

class WaitingDialog(private val activity: AppCompatActivity) {
    val dialog = Dialog(activity, R.style.Theme_FireKit_APP_DIALOG)
    private val txt: TextView
    private val img: ImageView

    init {
        dialog.setContentView(R.layout.row_waiting_popup)
        txt = dialog.findViewById(R.id.loading_txt)
        img = dialog.findViewById(R.id.videoView)
        runCatching {
            Glide.with(activity).asGif().load(R.drawable.loader2)
                .placeholder(ColorDrawable(Color.LTGRAY)).into(img)
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            dialog.window?.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            dialog.window?.setGravity(Gravity.CENTER)
        }.onFailure { it.printStackTrace() }
    }


    fun show() {
        runCatching { dialog.show() }.onFailure { it.printStackTrace() }
    }

    fun dismiss() {
        runCatching { dialog.dismiss() }.onFailure { it.printStackTrace() }
    }

    fun onDismiss(runnable: Runnable) {
        dialog.setOnDismissListener { runnable.run() }
    }
}
// keytool -list -v -alias key -keystore D:\Projects\Android\SmartNotify.jks -keypass DQusM4@M