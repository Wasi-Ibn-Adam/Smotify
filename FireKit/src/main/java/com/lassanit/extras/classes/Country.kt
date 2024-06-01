package com.lassanit.extras.classes

import android.annotation.SuppressLint
import android.content.Context
import java.util.Locale

class Country(var code: String, var name: String, var dialCode: Int) {
    fun getDisplayName(): String {
        return Locale("", code).getDisplayCountry(Locale.US)
    }

}