package com.lassanit.smotify.popups

import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.lassanit.smotify.R

class InfoPopup(context: AppCompatActivity, title: String, text: String?, html: Spanned? = null) {

    private val dialog: AlertDialog
    var view: View
    private var titleView: AppCompatTextView
    private var textView: AppCompatTextView
    private var btn: AppCompatButton
    private var cancel: AppCompatButton
    private var runnable: Runnable? = null

    init {
        view = LayoutInflater.from(context).inflate(R.layout.row_popup_card_info, null)
        val builder = AlertDialog.Builder(context)
        //builder.setTitle(title)
        //builder.setMessage(text)
        builder.setView(view)

        titleView = view.findViewById(R.id.popup_info_title)
        textView = view.findViewById(R.id.popup_info_text)
        btn = view.findViewById(R.id.popup_info_btn_next)
        cancel = view.findViewById(R.id.popup_info_btn_cancel)

        titleView.text = title
        textView.text = text ?: html


        dialog = builder.create()

        dialog.setCanceledOnTouchOutside(false)
        btn.setOnClickListener {
            dialog.dismiss()
            runnable?.run()
        }
        cancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    fun cancelVisibility(visibility: Boolean) {
        runCatching { cancel.visibility = if (visibility) View.VISIBLE else View.GONE }
    }

    fun show() {
        runCatching { dialog.show() }
    }

    fun dismiss(){
        runCatching { dialog.dismiss() }
    }
    fun onDismiss(runnable: Runnable) {
        runCatching { dialog.setOnDismissListener { runnable.run() } }
    }

    fun onNext(runnable: Runnable) {
        this.runnable = runnable
    }
}