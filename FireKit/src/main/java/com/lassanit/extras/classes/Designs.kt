package com.lassanit.extras.classes

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

class Designs(
    var activity: Activity? = null,
    var btn: Button? = null,
    var editText: EditText? = null,
    var textView: TextView? = null
) {

    class Activity(@DrawableRes var background: Int) {

    }

    class EditText(
        @DrawableRes var background: Int,
        @ColorRes var textColor: Int,
        @ColorRes var hintColor: Int,
        @ColorRes var drawableTint: Int
    ) {}

    class TextView(
        @ColorRes var textColor: Int,
    ) {}

    class Button(
        @DrawableRes var background: Int,
        @ColorRes var textColor: Int,
        @ColorRes var tintDrawable: Int
    ) {}


}