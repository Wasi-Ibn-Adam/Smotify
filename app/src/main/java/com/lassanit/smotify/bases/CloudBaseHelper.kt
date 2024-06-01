package com.lassanit.smotify.bases

import android.content.Context
import com.lassanit.smotify.bases.SmartBase.Companion.DB_VERSION

class CloudBaseHelper(private val sql:SmartBase) {
    companion object {
        const val DB_NAME="cloud_base_helper"
        fun getEditor(context: Context): EditBaseHelper {
            return SmartBase(context, DB_NAME, DB_VERSION).editHelper
        }
        fun getViewer(context: Context): DisplayBaseHelper {
            return SmartBase(context, DB_NAME, DB_VERSION).displayHelper
        }
        fun get(context: Context): SmartBase {
            return SmartBase(context, DB_NAME, DB_VERSION)
        }
    }
}