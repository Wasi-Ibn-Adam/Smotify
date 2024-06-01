package com.lassanit.smotify.popups

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import com.google.android.material.textfield.TextInputLayout
import com.lassanit.smotify.R

interface InputDialogImp {
    fun setName(name: String)
    fun setPassword(curr: String, new: String)
}


class AppInputDialog(
    context: Context,
    type: Type,
    private val imp: InputDialogImp
) :
    AppDialog(context) {
    enum class Type {
        NAME, PASSWORD
    }

    companion object {
        fun makeName(
            context: Context,
            name: String?,
            imp: InputDialogImp,
            limitChar: Int = 3
        ): AppInputDialog {
            return AppInputDialog(context, Type.NAME, imp).set(name, limitChar)
        }

        fun makePassword(context: Context, imp: InputDialogImp): AppInputDialog {
            return AppInputDialog(context, Type.PASSWORD, imp)
        }
    }


    private val allowB: AppCompatButton
    private lateinit var nameE: AppCompatEditText

    private lateinit var curPassL: TextInputLayout
    private lateinit var newPassL: TextInputLayout
    private lateinit var newConPassL: TextInputLayout
    private lateinit var curPassE: AppCompatEditText
    private lateinit var newPassE: AppCompatEditText
    private lateinit var newConPassE: AppCompatEditText
    private var minChar: Int = 3
    private val minPassword: Int = 8

    init {
        when (type) {
            Type.NAME -> {
                setContentView(R.layout.dialog_change_name)
                nameE = findViewById(R.id.dialog_change_name_text)
                allowB = findViewById(R.id.dialog_change_btn_update)
                allowB.setOnClickListener(getNameClickListener())
            }

            Type.PASSWORD -> {
                setContentView(R.layout.dialog_change_password)
                curPassE = findViewById(R.id.dialog_change_password_cur_text)
                newPassE = findViewById(R.id.dialog_change_password_text)
                newConPassE = findViewById(R.id.dialog_change_password_confirm_text)
                curPassL = findViewById(R.id.dialog_change_password_cur_text_lay)
                newPassL = findViewById(R.id.dialog_change_password_text_lay)
                newConPassL = findViewById(R.id.dialog_change_password_confirm_text_lay)
                handleError()
                allowB = findViewById(R.id.dialog_change_btn_update)
                allowB.setOnClickListener(getPasswordClickListener())
            }
        }
        findViewById<View>(R.id.dialog_change_btn_cancel).setOnClickListener { dismiss() }
        initFinalize()
    }

    private fun getNameClickListener(): OnClickListener {
        return OnClickListener {
            val res = nameE.text
            if (res.isNullOrBlank() || res.length < minChar) {
                nameE.error = "Invalid Input"
            } else {
                imp.setName(res.toString())
                dismiss()
            }
        }
    }

    private fun getPasswordClickListener(): OnClickListener {
        return OnClickListener {
            val curr = curPassE.text
            if (curr.isNullOrBlank() || curr.length < minPassword) {
                curPassL.error = "Min $minPassword Characters"
                return@OnClickListener
            }
            val new = newPassE.text
            if (new.isNullOrBlank() || new.length < minPassword) {
                newPassL.error = "Min $minPassword Characters"
                return@OnClickListener
            }
            val newC = newConPassE.text
            if (newC.isNullOrBlank() || newC.length < minPassword) {
                newConPassL.error = "Min $minPassword Characters"
                return@OnClickListener
            }

            if (newC.toString() != new.toString()) {
                newConPassL.error = "Password mis-match"
                return@OnClickListener
            }
            if (curr.toString() == new.toString()) {
                newPassL.error = "Password must be different"
                return@OnClickListener
            }

            imp.setPassword(curr.toString(), new.toString())
        }
    }

    private fun set(name: String?, limitChar: Int = 3): AppInputDialog {
        runCatching {
            nameE.setText(name)
            minChar = limitChar
        }
        return this
    }

    fun errorCur(err: String) {
        runCatching { curPassL.error = err }
    }

    fun errorNew(err: String) {
        runCatching { newPassL.error = err;newConPassL.error = err }
    }

    private fun handleError() {
        runCatching {
            curPassE.setOnClickListener {
                curPassL.error = null
                if (curPassL.isErrorEnabled)
                    curPassL.isErrorEnabled = false
            }
            newPassE.setOnClickListener {
                newPassL.error = null
                if (newPassL.isErrorEnabled)
                    newPassL.isErrorEnabled = false
            }

            newConPassE.setOnClickListener {
                newConPassL.error = null
                if (newConPassL.isErrorEnabled)
                    newConPassL.isErrorEnabled = false
            }
        }
    }

}