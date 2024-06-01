package com.lassanit.smotify.popups

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.lassanit.smotify.R

class InfoWindow(context: Context, title: String, text: String) : PopupWindow(context) {

    var view: View
    private var titleView: AppCompatTextView
    private var textView: AppCompatTextView
    private var btn: AppCompatButton
    private var cancel: AppCompatButton
    private var runnable: Runnable? = null

    init {
        view =
            LayoutInflater.from(context).inflate(R.layout.row_popup_card_info, null)
        animationStyle = android.R.anim.cycle_interpolator
        contentView = view
        isFocusable = false
        isOutsideTouchable = false

        setBackgroundDrawable(ColorDrawable(Color.RED))
        height = WindowManager.LayoutParams.WRAP_CONTENT
        width = WindowManager.LayoutParams.WRAP_CONTENT

        titleView = view.findViewById(R.id.popup_info_title)
        textView = view.findViewById(R.id.popup_info_text)
        btn = view.findViewById(R.id.popup_info_btn_next)
        cancel = view.findViewById(R.id.popup_info_btn_cancel)

        titleView.text = title
        textView.text = text

        btn.setOnClickListener {
            dismiss()
            runnable?.run()
        }
        cancel.setOnClickListener {
            dismiss()
        }
    }

    fun show() {
        runCatching { showAtLocation(view, Gravity.CENTER, 0, 0) }
    }

    fun onDismiss(runnable: Runnable) {
        runCatching { setOnDismissListener { runnable.run() } }
    }

    fun onNext(runnable: Runnable) {
        this.runnable = runnable
    }
}