package com.lassanit.smotify.bases

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getBlobOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.SmartBase.Companion.ALL
import com.lassanit.smotify.bases.SmartBase.Companion.AND
import com.lassanit.smotify.bases.SmartBase.Companion.CREATE_TAG
import com.lassanit.smotify.bases.SmartBase.Companion.EQUAL
import com.lassanit.smotify.bases.SmartBase.Companion.EQUAL_WHAT
import com.lassanit.smotify.bases.SmartBase.Companion.FROM
import com.lassanit.smotify.bases.SmartBase.Companion.GREATER_EQUAL
import com.lassanit.smotify.bases.SmartBase.Companion.GREATER_EQUAL_WHAT
import com.lassanit.smotify.bases.SmartBase.Companion.LIMIT
import com.lassanit.smotify.bases.SmartBase.Companion.ORDER
import com.lassanit.smotify.bases.SmartBase.Companion.ORDER_ASC
import com.lassanit.smotify.bases.SmartBase.Companion.ORDER_DSC
import com.lassanit.smotify.bases.SmartBase.Companion.SELECT
import com.lassanit.smotify.bases.SmartBase.Companion.SMALLER_EQUAL
import com.lassanit.smotify.bases.SmartBase.Companion.SMALLER_EQUAL_WHAT
import com.lassanit.smotify.bases.SmartBase.Companion.T_IMG
import com.lassanit.smotify.bases.SmartBase.Companion.T_INT
import com.lassanit.smotify.bases.SmartBase.Companion.T_LONG
import com.lassanit.smotify.bases.SmartBase.Companion.T_PRIME
import com.lassanit.smotify.bases.SmartBase.Companion.T_TEXT
import com.lassanit.smotify.bases.SmartBase.Companion.UPDATE_TAG
import com.lassanit.smotify.bases.SmartBase.Companion.WHERE
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.display.data.AppMessage

sealed interface AppMessageImp {
    fun has(msg: AppMessage): Pair<Int, Boolean>
    fun add(msg: AppMessage): Pair<Int, Boolean>
    fun updateMedia(mid: Int, name: String): Pair<Int, Boolean>

    fun hasAppMessage(msg: AppMessage): Int
    fun hasAppMessage(msg: AppMessage, sbnData: EditBaseHelper.SbnData?): Int

    fun hasAppMessageMessagingStyle(msg: AppMessage): Int
    fun hasAppMessagesMessagingStyle(msg: AppMessage): ArrayList<Int>


    fun addAppMessage(msg: AppMessage): Int
    fun addUniqueAppMessage(msg: AppMessage, sbnData: EditBaseHelper.SbnData?): Pair<Int, Boolean>
    fun addUniqueAppMessageMessageStyle(
        msg: AppMessage,
        sbnData: EditBaseHelper.SbnData?
    ): Pair<Int, Boolean>

    fun getAppMessage(mid: Int): AppMessage?
    fun getAppMessages(pkg: String, max: Int = -1, ascending: Boolean = true): ArrayList<AppMessage>
    fun getAppMessages(pkg: String, title: String, max: Int = -1, ascending: Boolean = true): ArrayList<AppMessage>

    fun deleteAppMessage(id: Int): Boolean
    fun deleteAppMessages(pkg: String): Boolean
    fun deleteAppMessages(pkg: String, title: String): Boolean

    fun readAllAppMessages(): Boolean
    fun readAppMessage(id: Int): Boolean
    fun readAppMessages(pkg: String): Boolean
    fun readAppMessages(pkg: String, title: String): Boolean

    fun getAppMessageId(sbnData: EditBaseHelper.SbnData): Int
    fun getUnreadAppMessagesCount(): Long
    fun updateMediaState(id: Int, mediaName: String): Boolean
}

class AppMessageBase(
    val context: Context,
    private val sql: SQLiteOpenHelper,
) : Base, AppMessageImp {

    object Params {
        private const val T_PKG = "com_lassaniT_smotify_"

        const val TABLE = T_PKG.plus("T_APP_MESSAGES")

        const val KEY_ID = T_PKG.plus("k_id")
        const val KEY_TYPE = T_PKG.plus("k_type")
        const val KEY_PKG = T_PKG.plus("k_pkg")
        const val KEY_TITLE = T_PKG.plus("k_title")
        const val KEY_TITLE_DESC = KEY_TITLE.plus("_desc")
        const val KEY_TEXT = T_PKG.plus("k_text")
        const val KEY_TEXT_DESC = KEY_TEXT.plus("_desc")
        const val KEY_TIME = T_PKG.plus("k_time")
        const val KEY_IS_UNREAD = T_PKG.plus("k_is_unread")
        const val KEY_HAS_MEDIA = T_PKG.plus("k_has_media")
        const val KEY_ICON = T_PKG.plus("k_icon")
        const val KEY_TAG = T_PKG.plus("k_tag")
        const val KEY_KEY = T_PKG.plus("k_key")

        ///////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////////////////////////////////////////////////////////////////////

        private const val KEY_ID_TAG = KEY_ID.plus(T_INT).plus(T_PRIME)
        private const val KEY_TYPE_TAG = KEY_TYPE.plus(T_INT)

        private const val KEY_PKG_TAG = KEY_PKG.plus(T_TEXT)

        private const val KEY_TITLE_TAG = KEY_TITLE.plus(T_TEXT)
        private const val KEY_TITLE_DESC_TAG = KEY_TITLE_DESC.plus(T_TEXT)
        private const val KEY_TEXT_TAG = KEY_TEXT.plus(T_TEXT)
        private const val KEY_TEXT_DESC_TAG = KEY_TEXT_DESC.plus(T_TEXT)
        private const val KEY_TIME_TAG = KEY_TIME.plus(T_LONG)
        private const val KEY_HAS_MEDIA_TAG = KEY_HAS_MEDIA.plus(T_INT)
        private const val KEY_IS_UNREAD_TAG = KEY_IS_UNREAD.plus(T_INT)
        private const val KEY_ICON_TAG = KEY_ICON.plus(T_IMG)

        const val KEY_TAG_TAG = KEY_TAG.plus(T_TEXT)
        const val KEY_KEY_TAG = KEY_KEY.plus(T_TEXT)
        ///////////////////////////////////////////////////////////////////////////////////////////

        val QUERY_APP_MESSAGES_TABLE_ = buildString {
            append(TABLE).append('(')
            append(KEY_ID_TAG).append(',')
            append(KEY_TYPE_TAG).append(',')
            append(KEY_PKG_TAG).append(',')
            append(KEY_TITLE_TAG).append(',')
            append(KEY_TITLE_DESC_TAG).append(',')
            append(KEY_TEXT_TAG).append(',')
            append(KEY_TEXT_DESC_TAG).append(',')
            append(KEY_TIME_TAG).append(',')
            append(KEY_HAS_MEDIA_TAG).append(',')
            append(KEY_IS_UNREAD_TAG).append(',')
            append(KEY_ICON_TAG).append(',')
            append(KEY_KEY_TAG).append(',')
            append(KEY_TAG_TAG).append(')')
        }


        fun queryHasGet(time: Long): String {
            return buildString {
                append(SELECT).append(ALL)
                append(FROM).append(TABLE).append(' ')
                append(WHERE).append(buildString {
                    append(KEY_PKG).append(EQUAL_WHAT).append(' ').append(AND)
                    append(KEY_TITLE).append(EQUAL_WHAT).append(' ').append(AND)
                    append(KEY_TITLE_DESC).append(EQUAL_WHAT).append(' ').append(AND)
                    append(KEY_TEXT).append(EQUAL_WHAT).append(' ').append(AND)
                    append(KEY_TEXT_DESC).append(EQUAL_WHAT).append(' ').append(AND)
                    append(KEY_TIME).append(EQUAL).append(time)
                })
            }
        }

        ///////////////////////////////////////////////////////////////////////////////////////////
        fun getQueryHasAppMessagesMessagingStyle(time: Long): String {
            return buildString {
                append(SELECT)
                append(KEY_ID).append(' ')
                append(FROM).append(TABLE).append(' ')
                append(WHERE).append(buildString {
                    append(KEY_PKG).append(' ').append(EQUAL_WHAT).append(' ').append(AND)
                    append(KEY_TITLE).append(' ').append(EQUAL_WHAT).append(' ').append(AND)
                    append(KEY_TEXT).append(' ').append(EQUAL_WHAT).append(' ').append(AND)
                    append(KEY_TITLE_DESC).append(' ').append(EQUAL_WHAT).append(' ').append(AND)
                    append(KEY_TEXT_DESC).append(' ').append(EQUAL_WHAT).append(' ').append(AND)
                    append(KEY_TIME).append(' ').append(SMALLER_EQUAL).append(time).append(' ')
                        .append(AND)
                    append(KEY_TIME).append(' ').append(GREATER_EQUAL).append(time.minus(500))
                })
            }
        }

        val QUERY_HAS_APP_MESSAGE = buildString {
            append(SELECT)
            append(KEY_ID).append(' ')
            append(FROM).append(TABLE).append(' ')
            append(WHERE).append(buildString {
                append(KEY_PKG).append(' ').append(EQUAL_WHAT).append(' ').append(AND)
                append(KEY_TITLE).append(' ').append(EQUAL_WHAT).append(' ').append(AND)
                append(KEY_TEXT).append(' ').append(EQUAL_WHAT).append(' ').append(AND)
                append(KEY_TIME).append(' ').append(GREATER_EQUAL_WHAT).append(' ').append(AND)
                append(KEY_TIME).append(' ').append(SMALLER_EQUAL_WHAT)
            })
        }
        val QUERY_HAS_APP_MESSAGE_UNIQUE = buildString {
            append(SELECT)
            append(KEY_ID).append(' ')
            append(FROM).append(TABLE).append(' ')
            append(WHERE).append(buildString {
                append(KEY_PKG).append(EQUAL_WHAT).append(AND)
                append(KEY_TITLE).append(EQUAL_WHAT).append(AND)
                append(KEY_TEXT).append(EQUAL_WHAT).append(AND)
                append(KEY_TIME).append(EQUAL_WHAT).append(AND)
                append(KEY_KEY).append(EQUAL_WHAT).append(AND)
                append(KEY_TAG).append(EQUAL_WHAT)
            })
        }

        private val QUERY_GET_APP_MESSAGES_HEADERS =
            buildString {
                append(SELECT).append(ALL)
                append(FROM).append(TABLE)
                append(WHERE).append(
                    buildString {
                        append(KEY_PKG).append(EQUAL_WHAT).append(AND)
                        append(KEY_TITLE).append(EQUAL_WHAT)
                    }
                ).append(' ')
                append(ORDER).append(KEY_TIME)
            }
        val QUERY_GET_APP_MESSAGES_ID_SBN =
            buildString {
                append(SELECT).append(KEY_ID).append(' ')
                append(FROM).append(TABLE).append(' ')
                append(WHERE).append(
                    buildString {
                        append(Params.KEY_KEY).append(EQUAL_WHAT).append(AND)
                        append(Params.KEY_TAG).append(EQUAL_WHAT)
                    }
                )
            }


        fun queryMessagesWithMedia(): String {
            return buildString {
                append(SELECT).append(KEY_ID).append(' ')
                append(FROM).append(TABLE).append(' ')
                append(WHERE).append(
                    buildString {
                        append(KEY_PKG).append(EQUAL_WHAT).append(' ').append(AND)
                        append(KEY_HAS_MEDIA).append(EQUAL).append(1)
                    }
                )
            }
        }

        /*-- If unread is true then unread count will be sent else all count --*/
        fun queryAppMessageCount(unread: Boolean = false): String {
            return buildString {
                append(SELECT).append("COUNT(*)")
                append(FROM).append(TABLE).append(' ')
                if (unread) {
                    append(WHERE).append(
                        buildString {
                            append(KEY_IS_UNREAD).append(" = ").append(1)
                        }
                    )
                }
            }
        }

        fun queryAppMessage(mid: Int): String {
            return buildString {
                append(SELECT).append(ALL)
                append(FROM).append(TABLE).append(' ')
                append(WHERE).append(
                    buildString {
                        append(KEY_ID).append(EQUAL).append(mid)
                    }
                )
            }
        }

        /*-- If since is 0 then all message will be provided and orderAsc define order --*/
        fun queryAppMessages(since: Long = 0L, orderAsc: Boolean = true): String {
            return buildString {
                append(SELECT).append(ALL)
                append(FROM).append(TABLE).append(' ')
                append(WHERE).append(
                    buildString {
                        append(KEY_TIME).append(GREATER_EQUAL).append(since)
                    }
                ).append(' ')
                append(ORDER).append(KEY_TIME)
                if (orderAsc)
                    append(ORDER_ASC)
                else
                    append(ORDER_DSC)
            }
        }

        /*-- if pkg is set null here in query pkg must be sent with arguments, orderAsc define order and max provide limit of list -1 means all --*/
        fun queryAppMessagesPkg(
            pkg: String? = null,
            orderAsc: Boolean = true,
            max: Int = -1
        ): String {
            return buildString {
                append(SELECT).append(ALL)
                append(FROM).append(TABLE).append(' ')
                append(WHERE).append(
                    buildString {
                        append(KEY_PKG)
                        if (pkg == null)
                            append(EQUAL_WHAT)
                        else
                            append(EQUAL).append("'$pkg'")
                    }
                ).append(' ')
                append(ORDER).append(KEY_TIME)
                if (orderAsc)
                    append(ORDER_ASC)
                else
                    append(ORDER_DSC)
                if (max > -1)
                    append(LIMIT).append(max)
            }
        }


        fun QUERY_GET_APP_MESSAGES_HEADER_ORDER(asc: Boolean, max: Int = -1): String {
            return if (max == -1)
                QUERY_GET_APP_MESSAGES_HEADERS.plus(if (asc) ORDER_ASC else ORDER_DSC)
            else
                QUERY_GET_APP_MESSAGES_HEADERS.plus(if (asc) ORDER_ASC else ORDER_DSC)
                    .plus(LIMIT).plus(max)
        }

    }

    companion object {
        @SuppressLint("Range")
        fun getAppMessage(cursor: Cursor): AppMessage {
            val id = cursor.getInt(cursor.getColumnIndex(Params.KEY_ID))
            val type = cursor.getInt(cursor.getColumnIndex(Params.KEY_TYPE))

            val pkg = cursor.getString(cursor.getColumnIndex(Params.KEY_PKG))

            val title = cursor.getString(cursor.getColumnIndex(Params.KEY_TITLE)).trim()
            val titleDes = cursor.getStringOrNull(cursor.getColumnIndex(Params.KEY_TITLE_DESC))

            val text = cursor.getString(cursor.getColumnIndex(Params.KEY_TEXT)).trim()
            val textDes = cursor.getStringOrNull(cursor.getColumnIndex(Params.KEY_TEXT_DESC))

            val time = cursor.getLong(cursor.getColumnIndex(Params.KEY_TIME))

            val hasMedia = cursor.getInt(cursor.getColumnIndex(Params.KEY_HAS_MEDIA)) == 1
            val isUnread = cursor.getInt(cursor.getColumnIndex(Params.KEY_IS_UNREAD)) == 1

            val icon =
                Resource.byteArrayToBitmap(cursor.getBlobOrNull(cursor.getColumnIndex(Params.KEY_ICON)))

            return AppMessage(
                id,
                type,
                pkg,
                title,
                titleDes,
                text,
                textDes,
                time,
                isUnread,
                hasMedia,
                icon
            )
        }

        fun getValues(msg: AppMessage, sbnData: EditBaseHelper.SbnData? = null): ContentValues {
            val values = ContentValues()

            values.put(Params.KEY_TYPE, msg.type)
            values.put(Params.KEY_PKG, msg.pkg.trim())

            values.put(Params.KEY_TITLE, msg.title.trim())
            values.put(Params.KEY_TITLE_DESC, (msg.titleDesc ?: "").trim())
            values.put(Params.KEY_TEXT, msg.text.trim())
            values.put(Params.KEY_TEXT_DESC, (msg.textDesc ?: "").trim())

            values.put(Params.KEY_TIME, msg.time)

            values.put(Params.KEY_HAS_MEDIA, if (msg.hasMedia) 1 else 0)
            values.put(Params.KEY_IS_UNREAD, if (msg.isUnread) 1 else 0)
            runCatching {
                if (msg.icon != null)
                    values.put(Params.KEY_ICON, Resource.bitmapToByteArray(msg.icon))
            }
            if (sbnData != null) {
                values.put(Params.KEY_KEY, sbnData.key.toString().trim())
                values.put(Params.KEY_TAG, sbnData.tag.toString().trim())
            }
            return values
        }
    }

    override fun onCreate(): String {
        return CREATE_TAG + Params.QUERY_APP_MESSAGES_TABLE_
    }

    override fun onUpgrade(): String {
        return UPDATE_TAG + Params.QUERY_APP_MESSAGES_TABLE_
    }

    fun updateTable(db: SQLiteDatabase) {
        if (SmartBase.columnExists(db, Params.TABLE, Params.KEY_KEY).not()) {
            db.execSQL("ALTER TABLE ${Params.TABLE} ADD COLUMN ${Params.KEY_KEY_TAG};");
        }
        if (SmartBase.columnExists(db, Params.TABLE, Params.KEY_TAG).not()) {
            db.execSQL("ALTER TABLE ${Params.TABLE} ADD COLUMN ${Params.KEY_TAG_TAG};");
        }
    }

    /**
     * @return Pair( id, hasMedia )
     * */
    @SuppressLint("Range")
    override fun has(msg: AppMessage): Pair<Int, Boolean> {
        var ret = Pair(-1, false)
        runCatching {
            sql.readableDatabase.use {
                it.rawQuery(
                    Params.queryHasGet(msg.time),
                    arrayOf(
                        msg.pkg.trim(),
                        msg.title.trim(),
                        (msg.titleDesc ?: "").trim(),
                        msg.text.trim(),
                        (msg.textDesc ?: "").trim()
                    )
                ).use { it1 ->
                    if (it1.moveToFirst()) {
                        val id = it1.getInt(it1.getColumnIndex(Params.KEY_ID))
                        val b = (it1.getInt(it1.getColumnIndex(Params.KEY_HAS_MEDIA)) == 1)
                        ret = Pair(id, b)
                        SmartNotify.log("$id, ${it1.count}", "SERVICE-DB")
                    }
                }
            }
        }.onFailure { it.printStackTrace() }
        return ret
    }

    /**
     * @return Pair( id, hasMedia )
     * */
    override fun add(msg: AppMessage): Pair<Int, Boolean> {
        var has = has(msg)
        runCatching {
            if (has.first == -1) {
                sql.writableDatabase.use {
                    val id = it.insert(Params.TABLE, null, getValues(msg, null)).toInt()
                    has = Pair(id, false)
                }
            }
        }.onFailure { it.printStackTrace() }
        return has
    }

    /**
     * @return Pair( id, hasMedia )
     * */
    override fun updateMedia(mid: Int, name: String): Pair<Int, Boolean> {
        var update = Pair(mid, false)
        runCatching {
            val msg = getAppMessage(mid)
            val desc = msg?.textDesc ?: ""
            val where = buildString { append(Params.KEY_ID).append(EQUAL).append(mid) }
            val values = ContentValues()
            values.put(Params.KEY_HAS_MEDIA, 1)
            values.put(
                Params.KEY_TEXT_DESC, desc
                    .replace("null", "")
                    .replace(Regex("\\(([^)]*\\d+)\\)"), "")
                    .plus("($name)")
            )
            sql.writableDatabase.use {
                update = Pair(mid, it.update(Params.TABLE, values, where, null) > 0)
            }
        }.onFailure { it.printStackTrace() }

        return update

    }


    override fun hasAppMessage(msg: AppMessage): Int {
        val db = sql.readableDatabase
        val cursor = db.rawQuery(
            Params.QUERY_HAS_APP_MESSAGE,
            arrayOf(
                msg.pkg,
                msg.title,
                msg.text,
                (msg.time - 1000).toString(),
                msg.time.toString()
            )
        )
        val id = if (cursor.moveToFirst()) cursor.getInt(0) else -1
        cursor.close()
        db.close()
        return id
    }

    override fun hasAppMessagesMessagingStyle(msg: AppMessage): ArrayList<Int> {
        return runCatching {
            val db = sql.readableDatabase
            val cursor = db.rawQuery(
                Params.getQueryHasAppMessagesMessagingStyle(msg.time),
                arrayOf(
                    msg.pkg,
                    msg.title,
                    msg.text,
                    msg.titleDesc.toString(),
                    msg.textDesc.toString()
                )
            )
            val list = ArrayList<Int>()
            if (cursor.moveToFirst()) {
                do {
                    list.add(cursor.getInt(0))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            list
        }.getOrElse { arrayListOf() }
    }

    override fun hasAppMessageMessagingStyle(msg: AppMessage): Int {
        return runCatching {
            val db = sql.readableDatabase
            val cursor = db.rawQuery(
                Params.getQueryHasAppMessagesMessagingStyle(msg.time),
                arrayOf(
                    msg.pkg,
                    msg.title,
                    msg.text,
                    msg.titleDesc.toString(),
                    msg.textDesc.toString()
                )
            )
            val id = if (cursor.moveToFirst()) cursor.getInt(0) else -1
            cursor.close()
            db.close()
            id
        }.getOrElse { -1 }
    }

    override fun hasAppMessage(msg: AppMessage, sbnData: EditBaseHelper.SbnData?): Int {
        if (sbnData == null) {
            return hasAppMessage(msg)
        }
        val db = sql.readableDatabase
        val cursor = db.rawQuery(
            Params.QUERY_HAS_APP_MESSAGE_UNIQUE,
            arrayOf(
                msg.pkg,
                msg.title,
                msg.text,
                msg.time.toString(),
                sbnData.key.toString(),
                sbnData.tag.toString()
            )
        )
        val id = if (cursor.moveToFirst()) cursor.getInt(0) else -1
        cursor.close()
        db.close()
        return id
    }

    override fun addAppMessage(msg: AppMessage): Int {
        var id = hasAppMessage(msg)
        if (id < 0) {
            val db = sql.writableDatabase
            id = db.insert(Params.TABLE, null, getValues(msg)).toInt()
            db.close()
        }
        return id
    }

    override fun addUniqueAppMessage(
        msg: AppMessage, sbnData: EditBaseHelper.SbnData?
    ): Pair<Int, Boolean> {
        var id = hasAppMessage(msg, sbnData)
        val new =
            if (id < 0) {
                val db = sql.writableDatabase
                id = db.insert(Params.TABLE, null, getValues(msg, sbnData)).toInt()
                db.close()
                true
            } else false
        return Pair(id, new)
    }

    override fun addUniqueAppMessageMessageStyle(
        msg: AppMessage, sbnData: EditBaseHelper.SbnData?
    ): Pair<Int, Boolean> {
        /*
        val ids = hasAppMessagesMessagingStyle(msg)
        var id = -1
        var add = true
        if (ids.isEmpty().not()) {
           // add = false

            for (oId in ids) {
                val oMsg = getAppMessage(oId) ?: continue
                SmartNotify.log(oMsg.toString(), "MsgBase-OLD")
                SmartNotify.log(msg.toString(), "MsgBase-NEW")
                SmartNotify.log("", "MsgBase")
                val oDesc = oMsg.textDesc
                val desc = msg.textDesc

                // oldEmp       newEmp         ==>>   add
                // oldEmp       newLoading     ==>>   add
                // oldEmp       new            ==>>   add

                // oldLoading   newEmp         ==>>   add
                // oldLoading   newLoading     ==>>   add
                // oldLoading   new            ==>>   update

                // old          newEmp         ==>>   add
                // old          newLoading     ==>>   add
                // old          new            ==>>    -       **
                if (oDesc == desc) {
                    add = false
                    id = oMsg.id
                    break
                }
                if (oDesc?.isNotBlank() == true) {
                    if (oDesc.endsWith("(Loading)", true)) {
                        if ((desc?.isNotBlank() == true)
                                .and(desc?.endsWith("(Loading)", true) == false)
                        ) {
                            // todo, update
                            val db = sql.writableDatabase
                            val contentValues = ContentValues()
                            contentValues.put(Params.KEY_TEXT_DESC, msg.textDesc.toString())
                            db.update(
                                Params.TABLE,
                                contentValues,
                                buildString { append(Params.KEY_ID).append(EQUAL).append(oId) },
                                null
                            )
                            db.close()
                            id = oId
                            add = false
                            break
                        }
                    }
                }
            }
        }
        if (add.or(id == -1)) {
            val db = sql.writableDatabase
            id = db.insert(Params.TABLE, null, getValues(msg, sbnData)).toInt()
            db.close()
        }
        return Pair(id, id < 0)
        */
        var id = hasAppMessageMessagingStyle(msg)
        val new =
            if (id < 0) {
                val db = sql.writableDatabase
                id = db.insert(Params.TABLE, null, getValues(msg, sbnData)).toInt()
                db.close()
                true
            } else false
        return Pair(id, new)
    }

    override fun getAppMessage(mid: Int): AppMessage? {
        val db = sql.readableDatabase
        val cursor =
            db.rawQuery(Params.queryAppMessage(mid), null)
        val msg = if (cursor.moveToFirst()) {
            getAppMessage(cursor)
        } else null
        cursor.close()
        db.close()
        return msg
    }

    override fun getAppMessages(pkg: String, max: Int, ascending: Boolean): ArrayList<AppMessage> {
        val db = sql.readableDatabase
        val cursor =
            db.rawQuery(Params.queryAppMessagesPkg(pkg, ascending, max), null)
        val list = ArrayList<AppMessage>()
        if (cursor.moveToFirst()) {
            do {
                list.add(getAppMessage(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    override fun getAppMessages(
        pkg: String, title: String, max: Int, ascending: Boolean
    ): ArrayList<AppMessage> {
        val db = sql.readableDatabase
        val cursor = db.rawQuery(
            Params.QUERY_GET_APP_MESSAGES_HEADER_ORDER(ascending, max),
            arrayOf(pkg, title)
        )
        val list = ArrayList<AppMessage>()
        if (cursor.moveToFirst()) {
            do {
                list.add(getAppMessage(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    override fun deleteAppMessage(id: Int): Boolean {
        val where = buildString {
            append(Params.KEY_ID).append('=').append(id)
        }
        val db = sql.writableDatabase
        val re = db.delete(Params.TABLE, where, null)
        db.close()
        return re > 0
    }

    override fun deleteAppMessages(pkg: String): Boolean {
        val where = buildString {
            append(Params.KEY_PKG).append(EQUAL_WHAT)
        }
        val db = sql.writableDatabase
        val re = db.delete(Params.TABLE, where, arrayOf(pkg))
        db.close()
        return re > 0
    }

    override fun deleteAppMessages(pkg: String, title: String): Boolean {
        val where = buildString {
            append(Params.KEY_PKG).append(EQUAL_WHAT).append(AND)
            append(Params.KEY_TITLE).append(EQUAL_WHAT)
        }
        val db = sql.writableDatabase
        val re = db.delete(Params.TABLE, where, arrayOf(pkg, title))
        db.close()
        return re > 0
    }

    override fun readAllAppMessages(): Boolean {
        val values = ContentValues()
        values.put(Params.KEY_IS_UNREAD, 0)
        val db = sql.writableDatabase
        val re = db.update(Params.TABLE, values, null, null)
        db.close()
        return re > 0
    }

    override fun readAppMessage(id: Int): Boolean {
        val where = buildString {
            append(Params.KEY_ID).append('=').append(id)
        }
        val values = ContentValues()
        values.put(Params.KEY_IS_UNREAD, 0)
        val db = sql.writableDatabase
        val re = db.update(Params.TABLE, values, where, null)
        db.close()
        return re > 0
    }

    override fun readAppMessages(pkg: String): Boolean {
        val where = buildString {
            append(Params.KEY_PKG).append(EQUAL_WHAT)
        }
        val values = ContentValues()
        values.put(Params.KEY_IS_UNREAD, 0)
        val db = sql.writableDatabase
        val re = db.update(Params.TABLE, values, where, arrayOf(pkg))
        db.close()
        return re > 0
    }

    override fun readAppMessages(pkg: String, title: String): Boolean {
        val where = buildString {
            append(Params.KEY_PKG).append(EQUAL_WHAT).append(AND)
            append(Params.KEY_TITLE).append(EQUAL_WHAT)
        }
        val values = ContentValues()
        values.put(Params.KEY_IS_UNREAD, 0)
        val db = sql.writableDatabase
        val re = db.update(Params.TABLE, values, where, arrayOf(pkg, title))
        db.close()
        return re > 0
    }

    override fun getAppMessageId(sbnData: EditBaseHelper.SbnData): Int {
        val db = sql.readableDatabase
        val cursor =
            db.rawQuery(Params.QUERY_GET_APP_MESSAGES_ID_SBN, arrayOf(sbnData.key, sbnData.tag))
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else -1
        cursor.close()
        db.close()
        return count
    }

    override fun getUnreadAppMessagesCount(): Long {
        val db = sql.readableDatabase
        val cursor = db.rawQuery(Params.queryAppMessageCount(true), null)
        val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        cursor.close()
        db.close()
        return count
    }

    override fun updateMediaState(id: Int, mediaName: String): Boolean {
        val desc = getAppMessage(id)?.textDesc ?: ""
        val where = buildString {
            append(Params.KEY_ID).append(EQUAL).append(id)
        }

        val values = ContentValues()
        values.put(Params.KEY_HAS_MEDIA, 1)
        values.put(Params.KEY_TEXT_DESC, desc.plus("($mediaName)"))

        val db = sql.writableDatabase
        val re = db.update(Params.TABLE, values, where, null)
        db.close()
        return re > 0
    }

    fun getAppMessagesCount(): Long {
        val db = sql.readableDatabase
        val cursor = db.rawQuery(Params.queryAppMessageCount(), null)
        val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        cursor.close()
        db.close()
        return count
    }

    fun getAllAppMessages(since: Long): ArrayList<AppMessage> {
        val db = sql.readableDatabase
        val cursor = db.rawQuery(
            Params.queryAppMessages(since),
            null
        )
        val list = ArrayList<AppMessage>()
        if (cursor.moveToFirst()) {
            do {
                list.add(getAppMessage(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun getMediaMessageIds(pkg: String): ArrayList<Int> {
        val db = sql.readableDatabase
        val cursor =
            db.rawQuery(Params.queryMessagesWithMedia(), arrayOf(pkg))
        val list = ArrayList<Int>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getIntOrNull(0)
                id?.let { list.add(it) }
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
}