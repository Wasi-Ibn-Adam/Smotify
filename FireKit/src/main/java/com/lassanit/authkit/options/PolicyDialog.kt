package com.lassanit.authkit.options

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lassanit.firekit.R

interface PolicyImp{
    fun onAgree()
    fun onError(e: Exception)
}

class PolicyDialog(context: Context,private val imp: PolicyImp?=null):Dialog(context,R.style.Theme_FireKit_APP_DIALOG) {
    companion object {
        private const val policyTermsConditions = "policy_and_term_conditioner"

        fun make(
            context: Context,
            callBacks: AuthKitOptions.CallBacks,
            imp: PolicyImp
        ): PolicyDialog {
            return PolicyDialog(context,imp).set(callBacks.onPolicy(), callBacks.onTermsAndConditions())
        }
    }
    private val pref =
        context.getSharedPreferences(context.packageName, AppCompatActivity.MODE_PRIVATE)

    init {
        setContentView(R.layout.container_user_policy_consent)
        setCanceledOnTouchOutside(false)
        setCancelable(false)
        window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        window?.setGravity(Gravity.BOTTOM)
    }

    fun set(onPolicy: Intent, onTerms: Intent): PolicyDialog {
        runCatching {
            findViewById<TextView>(R.id.txt_policy).setOnClickListener {
                context.startActivity(onPolicy)
            }
            findViewById<TextView>(R.id.txt_terms).setOnClickListener {
                context.startActivity(onTerms)
            }
        }.onFailure { imp?.onError(Exception(it)) }
        runCatching {
            val onClick = View.OnClickListener {
                pref.edit().putBoolean(policyTermsConditions, true).apply()
                dismiss()
                imp?.onAgree()
            }
            findViewById<View>(R.id.user_agree_consent_lay).setOnClickListener(onClick)
            findViewById<View>(R.id.user_agree_consent_img).setOnClickListener(onClick)
            findViewById<View>(R.id.user_agree_consent_text).setOnClickListener(onClick)
        }.onFailure { imp?.onError(Exception(it)) }
        return this
    }

    fun showIfConsentRequired() {
        runCatching{
            if (pref.getBoolean(policyTermsConditions, false)) {
                imp?.onAgree()
            } else {
                show()
            }
        }.onFailure { imp?.onError(Exception(it)) }
    }

}