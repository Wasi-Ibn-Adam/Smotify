package com.lassanit.authkit.options

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.lassanit.extras.classes.Designs
import com.lassanit.extras.customviews.PhoneText
import com.lassanit.extras.interfaces.DesignHandlerCallBacks

class DesignHandler(private var design: Designs) : DesignHandlerCallBacks {
    override fun setButtonDesign(view: View, id: Int) {
        try {
            setDesign(view.findViewById<Button>(id))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setTextViewDesign(view: View, id: Int) {
        try {
            setDesign(view.findViewById<TextView>(id))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setEdittextDesign(view: View, id: Int) {
        try {
            setDesign(view.findViewById<EditText>(id))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setDesign(textView: TextView?) {
        try {
            if (design.textView == null) return
            textView?.setTextColor(
                ContextCompat.getColor(
                    textView.context,
                    design.textView!!.textColor
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("UseCompatTextViewDrawableApis")
    override fun setDesign(editText: EditText?) {
        try {
            if (design.editText == null) return
            editText?.setBackgroundResource(design.editText!!.background)
            editText?.setTextColor(
                ContextCompat.getColor(
                    editText.context,
                    design.editText!!.textColor
                )
            )
            editText?.setHintTextColor(
                ContextCompat.getColor(
                    editText.context,
                    design.editText!!.hintColor
                )
            )
            editText?.compoundDrawableTintList =
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        editText!!.context,
                        design.editText!!.drawableTint
                    )
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setDesign(phoneText: PhoneText?) {
        try {
            phoneText?.setDesign(design.editText!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("UseCompatTextViewDrawableApis")
    override fun setDesign(button: Button?) {
        try {
            if (design.btn == null) return
            button?.setBackgroundResource(design.btn!!.background)
            button?.setTextColor(ContextCompat.getColor(button.context, design.btn!!.textColor))
            button?.compoundDrawableTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    button!!.context,
                    design.btn!!.tintDrawable
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}