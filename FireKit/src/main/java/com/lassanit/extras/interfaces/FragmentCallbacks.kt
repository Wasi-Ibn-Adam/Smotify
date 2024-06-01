package com.lassanit.extras.interfaces

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IdRes
import com.lassanit.extras.classes.Designs
import com.lassanit.extras.customviews.PhoneText
import com.lassanit.extras.fragments.SuperFragment
import com.lassanit.extras.classes.App
import com.lassanit.extras.classes.Company
import org.w3c.dom.Text

interface FragmentCallbacks {
    fun canAnimate(view: View)

    fun initApp(view: View)
    fun initCompany(view: View)
    fun initDesign(view: View)

    fun setApp(app: App): SuperFragment
    fun setCompany(company: Company?): SuperFragment
    fun setDesign(design: Designs): SuperFragment

    fun hideView(parent: View,@IdRes id:Int, gone: Boolean=true)

    fun getDefaultLinker(): HashMap<Int, View>
}