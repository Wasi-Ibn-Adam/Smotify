package com.lassanit.extras.interfaces

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IdRes
import com.lassanit.extras.customviews.PhoneText

interface DesignHandlerCallBacks {
    fun setButtonDesign(view: View, @IdRes id:Int)
    fun setEdittextDesign(view: View, @IdRes id:Int)
    fun setTextViewDesign(view: View, @IdRes id:Int)


    fun setDesign(button: Button?)
    fun setDesign(editText: EditText?)
    fun setDesign(textView: TextView?)
    fun setDesign(phoneText: PhoneText?)

}