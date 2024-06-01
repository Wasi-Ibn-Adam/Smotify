package com.lassanit.smotify.bases

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getStringOrNull
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.SmartBase.Companion.ALL
import com.lassanit.smotify.bases.SmartBase.Companion.AND
import com.lassanit.smotify.bases.SmartBase.Companion.AS
import com.lassanit.smotify.bases.SmartBase.Companion.COUNT
import com.lassanit.smotify.bases.SmartBase.Companion.EQUAL
import com.lassanit.smotify.bases.SmartBase.Companion.EQUAL_WHAT
import com.lassanit.smotify.bases.SmartBase.Companion.FROM
import com.lassanit.smotify.bases.SmartBase.Companion.GREATER
import com.lassanit.smotify.bases.SmartBase.Companion.GREATER_EQUAL
import com.lassanit.smotify.bases.SmartBase.Companion.GROUP
import com.lassanit.smotify.bases.SmartBase.Companion.IN
import com.lassanit.smotify.bases.SmartBase.Companion.LIMIT
import com.lassanit.smotify.bases.SmartBase.Companion.MAX
import com.lassanit.smotify.bases.SmartBase.Companion.ORDER
import com.lassanit.smotify.bases.SmartBase.Companion.ORDER_DSC
import com.lassanit.smotify.bases.SmartBase.Companion.SELECT
import com.lassanit.smotify.bases.SmartBase.Companion.SMALLER
import com.lassanit.smotify.bases.SmartBase.Companion.SMALLER_EQUAL
import com.lassanit.smotify.bases.SmartBase.Companion.SUM
import com.lassanit.smotify.bases.SmartBase.Companion.WHERE
import com.lassanit.smotify.bases.SmartBase.Companion.join
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.classes.SortWith
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.display.data.AppHeader
import com.lassanit.smotify.display.data.AppMessage
import com.lassanit.smotify.display.data.SmartMedia

sealed interface DisplayBaseImp {
    fun getSingleAppHeaders(): ArrayList<AppHeader>
    fun getSingleAppHeader(pkg: String): AppHeader?
    fun toggleAppHeaderVisibility(pkg: String): Boolean
    fun getSingleAppHeaderDetails(pkg: String, max: Int = -1): ArrayList<AppMessage>
    fun getAppMessages(
        pkg: String, previousMsg: AppMessage?, count: Int
    ): ArrayList<AppMessage>

    fun getSingleAppMessageMedia(msg: AppMessage): SmartMedia?
    fun getSize(pkg: String): Long
}

class DisplayBaseHelper(
    private val context: Context,
    private val sql: SQLiteOpenHelper,
    private val dbApp: AppBase,
    private val dbAppMessage: AppMessageBase,
    private val dbAppMedia: AppMediaBase
) : DisplayBaseImp {
    companion object {
        fun get(context: Context): DisplayBaseHelper {
            return SmartBase(context).displayHelper
        }
    }

    private object Params {
        /**
         * @since first arg must be pass pkg:String
         * @param visibility default = -1 and has no effect
         * @sample SharedBase.Visibility.SECRET
         * @sample SharedBase.Visibility.NORMAL
         * @return query to get selected app header
         * @see getAppHeaders
         * */
        fun getAppHeader(visibility: Int = -1): String {
            return buildString {
                append(SELECT).append(buildString {
                    append("a.${AppBase.Params.KEY_PKG}, ")
                    append("a.${AppBase.Params.KEY_PKG_NAME}, ")
                    append("a.${AppBase.Params.KEY_VISIBILITY}, ")
                    append(COUNT).append("(b.").append(AppMessageBase.Params.KEY_ID).append("), ")
                    append(SUM).append("(b.").append(AppMessageBase.Params.KEY_IS_UNREAD)
                        .append("), ")
                    append(MAX).append("(b.").append(AppMessageBase.Params.KEY_TIME).append(") ")
                }).append(' ')
                append(FROM).append(AppBase.Params.TABLE).append(' ').append(AS('a'))
                append(
                    join(
                        AppMessageBase.Params.TABLE, 'b',
                        "b.${AppMessageBase.Params.KEY_PKG}", "a.${AppBase.Params.KEY_PKG}"
                    )
                )
                append(WHERE).append(buildString {
                    append("a.${AppBase.Params.KEY_PKG}").append(EQUAL_WHAT)
                    if (visibility != -1)
                        append(' ').append(AND).append("a.${AppBase.Params.KEY_VISIBILITY}")
                            .append(EQUAL).append(visibility)
                }).append(' ')
                append(GROUP).append("b.${AppMessageBase.Params.KEY_PKG}")
            }
        }

        /**
         * @param visibility default = -1 and has no effect
         * @sample SharedBase.Visibility.SECRET
         * @sample SharedBase.Visibility.NORMAL
         * @return query to get all app headers
         * @see getAppHeader
         * */
        fun getAppHeaders(visibility: Int = -1): String {
            return buildString {
                append(SELECT).append(buildString {
                    append("a.${AppBase.Params.KEY_PKG}, ")
                    append("a.${AppBase.Params.KEY_PKG_NAME}, ")
                    append("a.${AppBase.Params.KEY_VISIBILITY}, ")
                    append(COUNT).append("(b.").append(AppMessageBase.Params.KEY_ID).append("), ")
                    append(SUM).append("(b.").append(AppMessageBase.Params.KEY_IS_UNREAD)
                        .append("), ")
                    append(MAX).append("(b.").append(AppMessageBase.Params.KEY_TIME).append(") ")
                }).append(' ')
                append(FROM).append(AppBase.Params.TABLE).append(' ').append(AS('a'))
                append(
                    join(
                        AppMessageBase.Params.TABLE, 'b',
                        "b.${AppMessageBase.Params.KEY_PKG}", "a.${AppBase.Params.KEY_PKG}"
                    )
                )
                if (visibility != -1)
                    append(WHERE).append(buildString {
                        append("a.${AppBase.Params.KEY_VISIBILITY}")
                            .append(EQUAL).append(visibility)
                    }).append(' ')
                append(GROUP).append("b.${AppMessageBase.Params.KEY_PKG}")
            }
        }

        /**
         * @param visibility default =-1 and has no effect
         * @param checkUnread default is false and return count of all messages entries
         * @return query that can get message cou to all apps
         * @sample SharedBase.Visibility.NORMAL
         * @sample SharedBase.Visibility.SECRET
         * @see getAppMessageCount
         * */
        fun getAppMessageCount(visibility: Int = -1, checkUnread: Boolean = false): String {
            return buildString {
                append(SELECT).append(COUNT).append('(').append(AppMessageBase.Params.KEY_ID)
                    .append(')').append(' ')
                append(FROM).append(AppMessageBase.Params.TABLE).append(' ')
                append(WHERE).append(buildString {
                    append(AppMessageBase.Params.KEY_PKG).append(' ').append(IN).append('(')
                    append(buildString {
                        append(SELECT).append(AppBase.Params.KEY_PKG).append(' ')
                        append(FROM).append(AppBase.Params.TABLE).append(' ')
                        if (visibility != -1)
                            append(WHERE).append(AppBase.Params.KEY_VISIBILITY).append(EQUAL)
                                .append(visibility).append(' ')
                        append(GROUP).append(AppBase.Params.KEY_PKG)
                    }).append(')')
                    if (checkUnread)
                        append(' ').append(AND).append(AppMessageBase.Params.KEY_IS_UNREAD)
                            .append(EQUAL).append(1)
                })
            }
        }

        /**
         * @since to use this query, first arg must be (pkg : String)
         * @see getAppMessageCount
         * @param checkUnread default is false , if true only count of unread will be return
         * @return query that can get message Count o specific App Package
         * */
        fun getAppMessageCount(checkUnread: Boolean = false): String {
            return buildString {
                append(SELECT).append(COUNT).append('(').append(AppMessageBase.Params.KEY_ID)
                    .append(')').append(' ')
                append(FROM).append(AppMessageBase.Params.TABLE).append(' ')
                append(WHERE).append(buildString {
                    append(AppMessageBase.Params.KEY_PKG).append(EQUAL_WHAT)
                    if (checkUnread)
                        append(' ').append(AND).append(AppMessageBase.Params.KEY_IS_UNREAD)
                            .append(EQUAL).append(1)
                })
            }
        }

        /**
         * @since to use this query, first arg must be (pkg : String)
         * @param limit default -1 and has no effect value must be positive
         * @see getQueryAppMessagesExcluded
         * @see getQueryAppMessagesIncluded
         * */
        fun getQueryAppMessages(limit: Int = -1): String {
            return buildString {
                append(SELECT).append(ALL).append(FROM).append(AppMessageBase.Params.TABLE)
                    .append(' ')
                append(WHERE).append(
                    buildString {
                        append(AppMessageBase.Params.KEY_PKG).append(EQUAL_WHAT)
                    }
                ).append(' ')
                append(ORDER).append(AppMessageBase.Params.KEY_TIME).append(' ').append(ORDER_DSC)
                if (limit != -1)
                    append(' ').append(LIMIT).append(limit)
            }
        }


        /**
         * @since to use this query, first arg must be (pkg : String)
         * @param limit default -1 and has no effect value must be positive
         * @param aboveId default -1 and has no effect value must be positive, this id will not be part in any case
         * @param belowId default -1 and has no effect value must be positive, this id will not be part in any case
         * @see getQueryAppMessages
         * @see getQueryAppMessagesIncluded
         * */
        fun getQueryAppMessagesExcluded(
            limit: Int = -1,
            aboveId: Int = -1,
            belowId: Int = -1
        ): String {
            return buildString {
                append(SELECT).append(ALL).append(FROM).append(AppMessageBase.Params.TABLE)
                    .append(' ')
                append(WHERE).append(
                    buildString {
                        append(AppMessageBase.Params.KEY_PKG).append(EQUAL_WHAT)
                        if (belowId != -1)
                            append(' ').append(AND)
                                .append(AppMessageBase.Params.KEY_ID).append(SMALLER)
                                .append(belowId)
                        if (aboveId != -1)
                            append(' ').append(AND)
                                .append(AppMessageBase.Params.KEY_ID).append(GREATER)
                                .append(aboveId)
                    }
                ).append(' ')
                append(ORDER).append(AppMessageBase.Params.KEY_TIME).append(' ').append(ORDER_DSC)
                if (limit != -1)
                    append(' ').append(LIMIT).append(limit)
            }
        }

        /**
         * @since to use this query, first arg must be (pkg : String)
         * @param limit default -1 and has no effect value must be positive
         * @param fromAboveId default -1 and has no effect value must be positive, this id will be a part of result if exist
         * @param fromBelowId default -1 and has no effect value must be positive, this id will be a part of result if exist
         * @see getQueryAppMessagesExcluded
         * @see getQueryAppMessages
         * */
        fun getQueryAppMessagesIncluded(
            limit: Int = -1,
            fromAboveId: Int = -1,
            fromBelowId: Int = -1
        ): String {
            return buildString {
                append(SELECT).append(ALL).append(FROM).append(AppMessageBase.Params.TABLE)
                    .append(' ')
                append(WHERE).append(
                    buildString {
                        append(AppMessageBase.Params.KEY_PKG).append(EQUAL_WHAT)
                        if (fromBelowId != -1)
                            append(' ').append(AND)
                                .append(AppMessageBase.Params.KEY_ID).append(SMALLER_EQUAL)
                                .append(fromBelowId)
                        if (fromAboveId != -1)
                            append(' ').append(AND)
                                .append(AppMessageBase.Params.KEY_ID).append(GREATER_EQUAL)
                                .append(fromAboveId)
                    }
                ).append(' ')
                append(ORDER).append(AppMessageBase.Params.KEY_TIME).append(' ').append(ORDER_DSC)
                if (limit != -1)
                    append(' ').append(LIMIT).append(limit)
            }
        }
    }

    fun getMessageList(since: Long): ArrayList<AppMessage> {
        return dbAppMessage.getAllAppMessages(since)
    }

    override fun getSingleAppHeaders(): ArrayList<AppHeader> {
        val list = ArrayList<AppHeader>()
        val db = sql.readableDatabase
        val cursor = db.rawQuery(
            Params.getAppHeaders(SharedBase.Visibility.Phone.isNormalOrSecret()), null
        )
        if (cursor.moveToFirst()) {
            do {
                try {
                    val pkg = cursor.getString(0)
                    val name = cursor.getStringOrNull(1) ?: Resource.getAppNameFromPackageName(
                        context,
                        pkg
                    ) ?: "App"
                    val visibility = cursor.getInt(2)
                    val total = cursor.getLong(3)
                    val unread = cursor.getLong(4)
                    val time = cursor.getLong(5)

                    list.add(AppHeader(pkg, name, visibility, total, unread, time))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        list.sortWith(SortWith.AppHeader)
        return list
    }

    override fun getSingleAppHeader(pkg: String): AppHeader? {
        val db = sql.readableDatabase
        val cursor = db.rawQuery(
            Params.getAppHeader(SharedBase.Visibility.Phone.isNormalOrSecret()),
            arrayOf(pkg)
        )
        val header =
            if (cursor.moveToFirst()) {
                try {
                    val name = cursor.getStringOrNull(1) ?: Resource.getAppNameFromPackageName(
                        context,
                        pkg
                    ) ?: "App"
                    val visibility = cursor.getInt(2)
                    val total = cursor.getLong(3)
                    val unread = cursor.getLong(4)
                    val time = cursor.getLong(5)
                    AppHeader(pkg, name, visibility, total, unread, time)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else {
                null
            }
        cursor.close()
        db.close()
        return header
    }

    override fun toggleAppHeaderVisibility(pkg: String): Boolean {
        val visibility = dbApp.getVisibility(pkg)
        val toggle =
            if (visibility == SharedBase.Visibility.NORMAL)
                SharedBase.Visibility.SECRET
            else
                SharedBase.Visibility.NORMAL
        return dbApp.setVisibility(pkg, toggle)
    }

    override fun getSingleAppHeaderDetails(pkg: String, max: Int): ArrayList<AppMessage> {
        val db = sql.readableDatabase
        val cursor = db.rawQuery(Params.getQueryAppMessages(max), arrayOf(pkg))
        val list = ArrayList<AppMessage>()
        if (cursor.moveToFirst()) {
            do {
                list.add(AppMessageBase.getAppMessage(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    override fun getAppMessages(
        pkg: String, previousMsg: AppMessage?, count: Int
    ): ArrayList<AppMessage> {
        if (previousMsg == null) {
            return getSingleAppHeaderDetails(pkg, count)
        }
        val db = sql.readableDatabase
        val cursor =
            db.rawQuery(
                Params.getQueryAppMessagesExcluded(count, belowId = previousMsg.id),
                arrayOf(pkg)
            )
        val list = ArrayList<AppMessage>()
        if (cursor.moveToFirst()) {
            do {
                list.add(AppMessageBase.getAppMessage(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        SmartNotify.log("$count ${list.size}, $pkg: ${previousMsg.pkg} ", "DISPLAY")
        return list
    }

    fun getAppMessages(
        pkg: String, previousMsg: AppMessage? = null, old: Boolean = true, count: Int = -1,
    ): ArrayList<AppMessage> {
        if (previousMsg == null) {
            return getSingleAppHeaderDetails(pkg, count)
        }
        val db = sql.readableDatabase
        val cursor =
            db.rawQuery(
                if (old)
                    Params.getQueryAppMessagesExcluded(count, belowId = previousMsg.id)
                else
                    Params.getQueryAppMessagesExcluded(count, aboveId = previousMsg.id),
                arrayOf(pkg)
            )
        val list = ArrayList<AppMessage>()
        if (cursor.moveToFirst()) {
            do {
                list.add(AppMessageBase.getAppMessage(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        SmartNotify.log("$count ${list.size}, $pkg: ${previousMsg.pkg} ", "DISPLAY")
        return list
    }

    fun getAppMessage(mid: Int): AppMessage? {
        return dbAppMessage.getAppMessage(mid)
    }

    override fun getSingleAppMessageMedia(msg: AppMessage): SmartMedia? {
        if (msg.hasMedia)
            return dbAppMedia.getMedia(msg.id)
        return null
    }

    override fun getSize(pkg: String): Long {
        val db = sql.readableDatabase
        val cursor = db.rawQuery(
            Params.getAppMessageCount(),
            arrayOf(pkg)
        )
        val count =
            if (cursor.moveToFirst()) {
                cursor.getLong(0)
            } else {
                0L
            }

        cursor.close()
        db.close()
        return count
    }

    fun getApp(pkg: String): App? {
        return dbApp.getApp(pkg)
    }

    fun getAppList(): ArrayList<App> {
        return dbApp.getApps()
    }

    fun getMedia(id: Int): SmartMedia? {
        return dbAppMedia.getMedia(id)
    }

    fun getUnreadMessagesCount(): Long {
        return runCatching {
            val db = sql.readableDatabase
            val cursor = db.rawQuery(
                Params.getAppMessageCount(SharedBase.Visibility.Phone.isNormalOrSecret(), true),
                null
            )
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            db.close()
            count
        }.onFailure {
            it.printStackTrace()
        }
            .getOrElse { 0L }
    }

    fun hasMessages(): Boolean {
        return dbAppMessage.getAppMessagesCount() > 0
    }

    fun getMediaList(lastMsgId: Long): ArrayList<SmartMedia> {
        return runCatching {
            dbAppMedia.getMedia(lastMsgId)
        }.getOrElse { arrayListOf() }
    }

    fun getMediaList(fromMsgId: Long, tillMsgId: Long): ArrayList<SmartMedia> {
        return runCatching {
            dbAppMedia.getMedia(fromMsgId, tillMsgId)
        }.getOrElse { arrayListOf() }

    }


}