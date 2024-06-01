package com.lassanit.smotify.bases

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

interface Base {
    fun onCreate(): String
    fun onUpgrade(): String
}

class SmartBase(val context: Context, db_name: String, db_version: Int) :
    SQLiteOpenHelper(context, db_name, null, db_version) {
    constructor(context: Context) : this(context, DB_NAME, DB_VERSION)

    private val dbApp = AppBase(context, this)
    private val dbAppMessage = AppMessageBase(context, this)
    private val dbAppMedia = AppMediaBase(context, this)

    val editHelper = EditBaseHelper(context, this, dbApp, dbAppMessage, dbAppMedia)
    val displayHelper = DisplayBaseHelper(context, this, dbApp, dbAppMessage, dbAppMedia)

    fun getExportBase():ExportBase.Helper{
        return ExportBase.Helper(context,dbApp, dbAppMessage, dbAppMedia)
    }

    override fun onCreate(p0: SQLiteDatabase?) {
        p0?.execSQL(dbApp.onCreate())
        p0?.execSQL(dbAppMessage.onCreate())
        p0?.execSQL(dbAppMedia.onCreate())
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        p0?.execSQL(dbApp.onUpgrade())
        p0?.execSQL(dbAppMessage.onUpgrade())
        p0?.execSQL(dbAppMedia.onUpgrade())
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    companion object {
        const val DB_VERSION = 5
        const val DB_NAME = "com_lassaniT_smotify_db_smart_bases"
        const val T_PRIME = " PRIMARY KEY"
        const val T_INT = " INTEGER"
        const val T_LONG = " LONG"
        const val T_TEXT = " TEXT"
        const val T_IMG = " BLOB"
        const val CREATE_TAG = "CREATE TABLE "
        const val UPDATE_TAG = "CREATE TABLE IF NOT EXISTS "

        const val SELECT = "SELECT "
        const val UPDATE = "UPDATE "
        const val DELETE = "DELETE "
        const val ALL = "* "
        const val COUNT = "COUNT"
        const val SUM = "SUM"
        const val MAX = "MAX"
        const val FROM = "FROM "
        const val JOIN = "JOIN "
        const val IN = "IN "
        fun join(table: String, aS: Char, onLeft: String, onRight: String): String {
            return JOIN.plus(table).plus(' ').plus(AS(aS)).plus("ON ").plus(onLeft)
                .plus('=')
                .plus(onRight).plus(' ')
        }

        fun AS(c: Char): String {
            return "AS ".plus(c).plus(' ')
        }


        const val SET = "SET "
        const val WHERE = "WHERE "
        const val AND = "AND "
        const val ORDER = "ORDER BY "
        const val ORDER_ASC = " ASC"
        const val ORDER_DSC = " DESC"
        const val GROUP = "GROUP BY "
        const val LIMIT = "LIMIT "
        const val BETWEEN = "BETWEEN "
        const val EQUAL = "="
        const val EQUAL_WHAT = "=?"
        const val GREATER = ">"
        const val GREATER_WHAT = ">?"
        const val GREATER_EQUAL = ">="
        const val GREATER_EQUAL_WHAT = ">=?"
        const val SMALLER = "<"
        const val SMALLER_WHAT = "<?"
        const val SMALLER_EQUAL = "<="
        const val SMALLER_EQUAL_WHAT = "<=?"

        @SuppressLint("Range")
        fun columnExists(db: SQLiteDatabase, tableName: String, columnName: String): Boolean {
            val cursor = db.rawQuery(
                "PRAGMA table_info($tableName)", null
            )
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val existingColumnName = cursor.getString(cursor.getColumnIndex("name"))
                    if (columnName == existingColumnName) {
                        cursor.close()
                        return true
                    }
                }
                cursor.close()
            }
            return false
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////


}