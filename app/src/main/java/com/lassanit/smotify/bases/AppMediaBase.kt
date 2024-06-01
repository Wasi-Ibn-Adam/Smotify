package com.lassanit.smotify.bases

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.database.getBlobOrNull
import androidx.core.database.getStringOrNull
import com.lassanit.smotify.bases.SmartBase.Companion.ALL
import com.lassanit.smotify.bases.SmartBase.Companion.AND
import com.lassanit.smotify.bases.SmartBase.Companion.BETWEEN
import com.lassanit.smotify.bases.SmartBase.Companion.CREATE_TAG
import com.lassanit.smotify.bases.SmartBase.Companion.FROM
import com.lassanit.smotify.bases.SmartBase.Companion.GREATER
import com.lassanit.smotify.bases.SmartBase.Companion.SELECT
import com.lassanit.smotify.bases.SmartBase.Companion.T_IMG
import com.lassanit.smotify.bases.SmartBase.Companion.T_INT
import com.lassanit.smotify.bases.SmartBase.Companion.T_PRIME
import com.lassanit.smotify.bases.SmartBase.Companion.T_TEXT
import com.lassanit.smotify.bases.SmartBase.Companion.UPDATE_TAG
import com.lassanit.smotify.bases.SmartBase.Companion.WHERE
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.display.data.SmartMedia
import com.lassanit.smotify.services_receivers.SmartPhoneService


sealed interface AppMediaImp {
    fun hasMedia(mid: Int): Int
    fun addMedia(mid: Int, map: Bitmap): Int
    fun addMedia(mid: Int, uri: Uri): Int
    fun addMedia(mid: Int, media: SmartMedia): Int
    fun getMedia(mid: Int): SmartMedia?
    fun getMedia(): ArrayList<SmartMedia>
}

class AppMediaBase(val context: Context, private val sql: SmartBase) : Base, AppMediaImp {
    object Params {
        private const val T_PKG = "com_lassaniT_smotify_"

        const val TABLE = T_PKG.plus("T_APP_IMGS")

        const val KEY_ID = T_PKG.plus("k_id")
        const val KEY_MID = T_PKG.plus("k_app_message_id")
        const val KEY_IMAGE = T_PKG.plus("k_img")
        const val KEY_URI = T_PKG.plus("k_img_uri")

        private const val KEY_ID_TAG = KEY_ID.plus(T_INT).plus(T_PRIME)
        private const val KEY_MID_TAG = KEY_MID.plus(T_INT)
        private const val KEY_IMAGE_TAG = KEY_IMAGE.plus(T_IMG)
        private const val KEY_URI_TAG = KEY_URI.plus(T_TEXT)

        const val QUERY_APP_IMAGES_TABLE_ =
            "$TABLE ($KEY_ID_TAG, $KEY_MID_TAG, $KEY_IMAGE_TAG, $KEY_URI_TAG)"
    }

    companion object {
        @SuppressLint("Range")
        fun getMedia(cursor: Cursor): SmartMedia {
            val barry = cursor.getBlobOrNull(cursor.getColumnIndex(Params.KEY_IMAGE))
            val map = Resource.byteArrayToBitmap(barry)
            val uriStr =
                cursor.getStringOrNull(cursor.getColumnIndex(Params.KEY_URI))?.trim()
            val uri: Uri? = if (SmartPhoneService.Utils.isEmpty(uriStr)) null else Uri.parse(uriStr)
            val mid = cursor.getInt(cursor.getColumnIndex(Params.KEY_MID))
            return SmartMedia(mid, map, uri)
        }

        fun getValues(media: SmartMedia): ContentValues {
            val values = ContentValues()
            if (media.getMap() != null) {
                values.put(Params.KEY_IMAGE, Resource.bitmapToByteArray(media.getMap()!!))
            }
            if (media.getUri() != null) {
                values.put(Params.KEY_URI, media.getUri().toString().trim())
            }
            values.put(Params.KEY_MID, media.mid)
            return values
        }
    }

    override fun onCreate(): String {
        return CREATE_TAG + Params.QUERY_APP_IMAGES_TABLE_
    }

    override fun onUpgrade(): String {
        return UPDATE_TAG + Params.QUERY_APP_IMAGES_TABLE_
    }

    override fun hasMedia(mid: Int): Int {
        val db = sql.readableDatabase
        val query =
            "SELECT ${Params.KEY_ID} FROM ${Params.TABLE} WHERE ${Params.KEY_MID}=${mid}"
        val cursor = db.rawQuery(query, null)
        val id = if (cursor.moveToFirst()) cursor.getInt(0) else -1
        cursor.close()
        db.close()
        return id
    }

    override fun addMedia(mid: Int, map: Bitmap): Int {
        var id = hasMedia(mid)
        if (id < 0) {
            val db = sql.writableDatabase
            id = db.insert(Params.TABLE, null, getValues(SmartMedia(mid, map, null))).toInt()
            db.close()
        }
        return id
    }

    override fun addMedia(mid: Int, uri: Uri): Int {
        var id = hasMedia(mid)
        if (id < 0) {
            val db = sql.writableDatabase
            id = db.insert(Params.TABLE, null, getValues(SmartMedia(mid, null, uri))).toInt()
            db.close()
        }
        return id
    }

    override fun addMedia(mid: Int, media: SmartMedia): Int {
        var id = hasMedia(mid)
        if (id < 0) {
            media.mid = mid
            val db = sql.writableDatabase
            id = db.insert(Params.TABLE, null, getValues(media)).toInt()
            db.close()
        }
        return id
    }

    override fun getMedia(mid: Int): SmartMedia? {
        val query =
            "SELECT * FROM ${Params.TABLE} WHERE ${Params.KEY_MID}=$mid"
        val db = sql.readableDatabase
        val cursor = db.rawQuery(query, null)
        val media = if (cursor.moveToFirst()) getMedia(cursor) else null
        cursor.close()
        db.close()
        return media
    }

    override fun getMedia(): ArrayList<SmartMedia> {
        val query =
            "SELECT * FROM ${Params.TABLE}"
        val db = sql.readableDatabase
        val cursor = db.rawQuery(query, null)
        val list = ArrayList<SmartMedia>()
        if (cursor.moveToFirst()) {
            do {
                list.add(getMedia(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun getMedia(sinceMsgId: Long): ArrayList<SmartMedia> {
        val query =
            "SELECT * FROM ${Params.TABLE}"
        buildString {
            append(SELECT).append(' ').append(ALL).append(' ')
            append(FROM).append(Params.TABLE).append(' ')
            append(WHERE).append(
                buildString {
                    append(Params.KEY_MID).append(' ')
                    append(GREATER).append(' ').append(sinceMsgId)
                }
            )
        }
        val db = sql.readableDatabase
        val cursor = db.rawQuery(query, null)
        val list = ArrayList<SmartMedia>()
        if (cursor.moveToFirst()) {
            do {
                list.add(getMedia(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun getMedia(fromMsg: Long, tillMsg: Long): ArrayList<SmartMedia> {
        val query =
            buildString {
                append(SELECT).append(' ').append(ALL).append(' ')
                append(FROM).append(Params.TABLE).append(' ')
                append(WHERE).append(
                    buildString {
                        append(Params.KEY_MID).append(' ')
                        append(BETWEEN).append(fromMsg).append(' ')
                        append(AND).append(tillMsg)
                    }
                )
            }
        val db = sql.readableDatabase
        val cursor = db.rawQuery(query, null)
        val list = ArrayList<SmartMedia>()
        if (cursor.moveToFirst()) {
            do {
                list.add(getMedia(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

}