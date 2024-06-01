package com.lassanit.smotify.bases

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.lassanit.smotify.bases.SmartBase.Companion.CREATE_TAG
import com.lassanit.smotify.bases.SmartBase.Companion.T_INT
import com.lassanit.smotify.bases.SmartBase.Companion.T_PRIME
import com.lassanit.smotify.bases.SmartBase.Companion.T_TEXT
import com.lassanit.smotify.bases.SmartBase.Companion.UPDATE_TAG
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.classes.SortWith
import com.lassanit.smotify.display.data.App

interface AppDisplayImp {
    fun hasApp(app: App): Int
    fun hasApp(pkg: String): Int
    fun hasApp(pkg: String, name: String): Int

    fun getVisibility(id: Int): Int
    fun getVisibility(pkg: String): Int
    fun getVisibility(pkg: String, name: String): Int

    fun getApp(id: Int): App?
    fun getApp(pkg: String): App?
    fun getApp(pkg: String, name: String): App?

    fun getApps(): ArrayList<App>
    fun getAppsVisible(): ArrayList<App>
    fun getAppsHidden(): ArrayList<App>
}

interface AppEditImp {
    fun addApp(app: App): Int
    fun addApp(pkg: String): Int
    fun addApp(pkg: String, name: String): Int


    fun setVisibility(id: Int, visibility: Int): Boolean
    fun setVisibility(pkg: String, visibility: Int): Boolean
    fun setVisibility(pkg: String, name: String, visibility: Int): Boolean
}

sealed interface AppImp : AppEditImp, AppDisplayImp {
}

class AppBase(val context: Context, private val sql: SmartBase) : Base, AppImp {
    object Params {
        private const val T_PKG = "com_lassaniT_smotify_"

        const val TABLE = T_PKG.plus("T_APP")

        const val KEY_ID = T_PKG.plus("k_id")
        const val KEY_PKG = T_PKG.plus("k_pkg")
        const val KEY_PKG_NAME = T_PKG.plus("k_pkg_name")
        const val KEY_VISIBILITY = T_PKG.plus("k_visibility")

        private const val KEY_ID_TAG = KEY_ID.plus(T_INT).plus(T_PRIME)
        private const val KEY_PKG_TAG = KEY_PKG.plus(T_TEXT)
        private const val KEY_PKG_NAME_TAG = KEY_PKG_NAME.plus(T_TEXT)
        private const val KEY_VISIBILITY_TAG = KEY_VISIBILITY.plus(T_INT)

        const val QUERY_APP_TABLE_ =
            "$TABLE ($KEY_ID_TAG, $KEY_PKG_TAG, $KEY_PKG_NAME_TAG, $KEY_VISIBILITY_TAG)"
    }

    companion object {
        @SuppressLint("Range")
        fun getApp(cursor: Cursor): App {
            val pkg = (cursor.getString(cursor.getColumnIndex(Params.KEY_PKG)))
            val id = (cursor.getInt(cursor.getColumnIndex(Params.KEY_ID)))
            val name = (cursor.getString(cursor.getColumnIndex(Params.KEY_PKG_NAME)))
            val visibility =
                (cursor.getInt(cursor.getColumnIndex(Params.KEY_VISIBILITY)))
            return App(pkg, name, visibility, id)
        }
    }

    override fun onCreate(): String {
        return CREATE_TAG + Params.QUERY_APP_TABLE_
    }

    override fun onUpgrade(): String {
        return UPDATE_TAG + Params.QUERY_APP_TABLE_
    }

    override fun addApp(app: App): Int {
        var id = hasApp(app.pkg)
        if (id < 0) {
            val db = sql.writableDatabase
            val values = ContentValues()
            values.put(Params.KEY_PKG, app.pkg)
            values.put(Params.KEY_PKG_NAME, app.name)
            values.put(Params.KEY_VISIBILITY, app.visibility)
            id = db.insert(Params.TABLE, null, values).toInt()
            db.close()
        }
        return id
    }

    override fun addApp(pkg: String): Int {
        return addApp(pkg, Resource.getAppNameFromPackageName(context, pkg) ?: "App")
    }

    override fun addApp(pkg: String, name: String): Int {
        return addApp(App(pkg, name))
    }

    override fun hasApp(app: App): Int {
        return hasApp(app.pkg, app.name)
    }

    override fun hasApp(pkg: String): Int {
        val db = sql.readableDatabase
        val query =
            "SELECT ${Params.KEY_ID} FROM ${Params.TABLE} WHERE ${Params.KEY_PKG}=?"
        val cursor = db.rawQuery(query, arrayOf(pkg))
        val state = if (cursor.moveToFirst()) cursor.getInt(0) else -1
        cursor.close()
        db.close()
        return state
    }

    override fun hasApp(pkg: String, name: String): Int {
        val db = sql.readableDatabase
        val query =
            "SELECT ${Params.KEY_ID} FROM ${Params.TABLE} WHERE ${Params.KEY_PKG}=? AND ${Params.KEY_PKG_NAME}=?"
        val cursor = db.rawQuery(query, arrayOf(pkg, name))
        val state = if (cursor.moveToFirst()) cursor.getInt(0) else -1
        cursor.close()
        db.close()
        return state
    }

    override fun getVisibility(id: Int): Int {
        val db = sql.readableDatabase
        val query =
            "SELECT ${Params.KEY_VISIBILITY} FROM ${Params.TABLE} WHERE ${Params.KEY_ID}=$id"
        val cursor = db.rawQuery(query, null)
        val visibility =
            if (cursor.moveToFirst()) cursor.getInt(0) else SharedBase.Visibility.NORMAL
        cursor.close()
        db.close()
        return visibility
    }

    override fun getVisibility(pkg: String): Int {
        val db = sql.readableDatabase
        val query =
            "SELECT ${Params.KEY_VISIBILITY} FROM ${Params.TABLE} WHERE ${Params.KEY_PKG}=?"
        val cursor = db.rawQuery(query, arrayOf(pkg))

        val visibility =
            if (cursor.moveToFirst()) cursor.getInt(0) else SharedBase.Visibility.NORMAL
        cursor.close()
        db.close()
        return visibility
    }

    override fun getVisibility(pkg: String, name: String): Int {
        val db = sql.readableDatabase
        val query =
            "SELECT ${Params.KEY_VISIBILITY} FROM ${Params.TABLE} WHERE ${Params.KEY_PKG}=? AND ${Params.KEY_PKG_NAME}=?"
        val cursor = db.rawQuery(query, arrayOf(pkg, name))

        val visibility =
            if (cursor.moveToFirst()) cursor.getInt(0) else SharedBase.Visibility.NORMAL
        cursor.close()
        db.close()
        return visibility
    }

    override fun setVisibility(id: Int, visibility: Int): Boolean {
        val values = ContentValues()
        values.put(Params.KEY_VISIBILITY, visibility)
        val db = sql.writableDatabase
        val count = db.update(
            Params.TABLE,
            values,
            "${Params.KEY_ID}=$id",
            null
        )
        db.close()
        return count > 0
    }

    override fun setVisibility(pkg: String, visibility: Int): Boolean {
        val values = ContentValues()
        values.put(Params.KEY_VISIBILITY, visibility)
        val db = sql.writableDatabase
        val count = db.update(
            Params.TABLE,
            values,
            "${Params.KEY_PKG}=?",
            arrayOf(pkg)
        )
        db.close()
        return count > 0
    }

    override fun setVisibility(pkg: String, name: String, visibility: Int): Boolean {
        val values = ContentValues()
        values.put(Params.KEY_VISIBILITY, visibility)
        val db = sql.writableDatabase
        val count = db.update(
            Params.TABLE,
            values,
            "${Params.KEY_PKG}=? AND ${Params.KEY_PKG_NAME}=?",
            arrayOf(pkg, name)
        )
        db.close()
        return count > 0
    }


    override fun getApp(id: Int): App? {
        val db = sql.readableDatabase
        val query =
            "SELECT * FROM ${Params.TABLE} WHERE ${Params.KEY_ID}=$id"
        val cursor = db.rawQuery(query, null)
        val app = if (cursor.moveToFirst()) getApp(cursor) else null
        cursor.close()
        db.close()
        return app
    }

    override fun getApp(pkg: String): App? {
        val db = sql.readableDatabase
        val query =
            "SELECT * FROM ${Params.TABLE} WHERE ${Params.KEY_PKG}=?"
        val cursor = db.rawQuery(query, arrayOf(pkg))
        val app = if (cursor.moveToFirst()) getApp(cursor) else null
        cursor.close()
        db.close()
        return app
    }

    override fun getApp(pkg: String, name: String): App? {
        val db = sql.readableDatabase
        val query =
            "SELECT * FROM ${Params.TABLE} WHERE ${Params.KEY_PKG}=? AND ${Params.KEY_PKG_NAME}=?"
        val cursor = db.rawQuery(query, arrayOf(pkg, name))
        val app = if (cursor.moveToFirst()) getApp(cursor) else null
        cursor.close()
        db.close()
        return app
    }

    override fun getApps(): ArrayList<App> {
        val list = ArrayList<App>()
        val db = sql.readableDatabase
        val query = "SELECT * FROM ${Params.TABLE}"
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(getApp(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        list.sortWith(SortWith.SmartApp)
        return list
    }

    override fun getAppsVisible(): ArrayList<App> {
        val list = ArrayList<App>()
        val db = sql.readableDatabase
        val query =
            "SELECT * FROM ${Params.TABLE} WHERE ${Params.KEY_VISIBILITY}=${SharedBase.Visibility.NORMAL}"
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(getApp(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        list.sortWith(SortWith.SmartApp)
        return list
    }

    override fun getAppsHidden(): ArrayList<App> {
        val list = ArrayList<App>()
        val db = sql.readableDatabase
        val query =
            "SELECT * FROM ${Params.TABLE} WHERE ${Params.KEY_VISIBILITY}=${SharedBase.Visibility.SECRET}"
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(getApp(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        list.sortWith(SortWith.SmartApp)
        return list
    }

}