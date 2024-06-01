package com.lassanit.extras.interfaces

import android.view.View

interface AppCallbacks {
    fun onFragmentLoaded(str: String, view: View)
    fun onFragmentComplete(str: String)
    fun backPress(allow: Boolean)
    fun loadingPopup(show: Boolean)
    fun log(any: Any)
}