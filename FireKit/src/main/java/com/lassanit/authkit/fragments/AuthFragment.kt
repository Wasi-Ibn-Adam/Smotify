package com.lassanit.authkit.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.google.android.material.textfield.TextInputLayout
import com.lassanit.authkit.interfaces.AuthActions
import com.lassanit.extras.customviews.PhoneText
import com.lassanit.extras.fragments.SuperFragment
import com.lassanit.firekit.R


open class AuthFragment(@LayoutRes private var res: Int) : SuperFragment(res) {
    protected lateinit var actions: AuthActions
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AuthActions) {
            actions = context
        } else {
            throw IllegalArgumentException("Activity must implement AuthActions")
        }
    }

    override fun canAnimate(view: View) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try{
            hideView(view,R.id.fireKit_app_logo,requireActivity().isInMultiWindowMode)
        }
        catch (e:Exception){
            e.printStackTrace()
        }
    }

    override fun initDesign(view: View) {
        try {
            designHandler.setButtonDesign(view, R.id.fireKit_button_default)
            designHandler.setButtonDesign(view, R.id.fireKit_button_small)
            designHandler.setEdittextDesign(view, R.id.fireKit_editText_email)
            designHandler.setEdittextDesign(view, R.id.fireKit_editText_password)
            designHandler.setEdittextDesign(view, R.id.fireKit_editText_passwordConfirm)
            designHandler.setEdittextDesign(view, R.id.fireKit_phoneText)
            designHandler.setDesign(view.findViewById<PhoneText>(R.id.fireKit_phoneText))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getDefaultLinker(): HashMap<Int, View> {
        val map = super.getDefaultLinker()
        val img = view?.findViewById<ImageView>(R.id.fireKit_app_logo)
        if (img != null)
            map[R.string.tag_app_logo] = (img)
        val txt = view?.findViewById<TextView>(R.id.fireKit_app_name)
        if (txt != null)
            map[R.string.tag_app_name] = (txt)
        return map
    }


    protected fun handleError(lay: TextInputLayout, editText: EditText) {
        runCatching {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    runCatching {
                        if (lay.isErrorEnabled)
                            lay.isErrorEnabled = false
                    }
                }

                override fun afterTextChanged(p0: Editable?) {
                    runCatching {
                        if (lay.isErrorEnabled)
                            lay.isErrorEnabled = false
                    }
                }
            })
        }
    }


}