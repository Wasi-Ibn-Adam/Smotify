package com.lassanit.smotify.popups

import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.lassanit.smotify.R

class AppPermissionDialog(
    private val activity: FragmentActivity,
    title: String,
    text: String?,
    pT: String,
    nT: String
) : AppDialog(activity) {
    companion object {
        val resultPermissions = AppResultPermissionDialog.Companion

        fun ask(
            activity: FragmentActivity, title: String,
            text: String?,
            pT: String? = null,
            nT: String? = null
        ): AppPermissionDialog {
            return AppPermissionDialog(
                activity, title, text, pT ?: "Allow", nT ?: "Cancel"
            )
        }
    }


    private var plusR: Runnable? = null
    private var minusR: Runnable? = null

    private val yesB: AppCompatButton
    private val noB: AppCompatButton
    private val titleT: AppCompatTextView
    private val descT: AppCompatTextView
    private var dismissOnActionPositive: Boolean = true

    init {
        setContentView(R.layout.dialog_app_permission)

        yesB = findViewById(R.id.dialog_app_permission_btn_yes)
        noB = findViewById(R.id.dialog_app_permission_btn_no)
        titleT = findViewById(R.id.dialog_app_permission_title)
        descT = findViewById(R.id.dialog_app_permission_description)

        yesB.text = pT
        noB.text = nT

        titleT.text = title
        descT.text = text

        yesB.setOnClickListener {
            runCatching {
                activity.runOnUiThread {
                    plusR?.run()
                }
            }
            if (dismissOnActionPositive)
                dismiss()
        }
        noB.setOnClickListener { runCatching { minusR?.run() };dismiss() }

        initFinalize()
    }

    fun set(
        onPositive: Runnable?,
        onNegative: Runnable?,
        dismissOnActionPositive: Boolean = true
    ): AppPermissionDialog {
        this.dismissOnActionPositive = dismissOnActionPositive
        plusR = onPositive
        minusR = onNegative
        return this
    }
}