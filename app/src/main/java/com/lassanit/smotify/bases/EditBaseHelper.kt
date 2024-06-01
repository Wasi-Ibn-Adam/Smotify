package com.lassanit.smotify.bases

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.SmartNotify.Companion.log
import com.lassanit.smotify.bases.SmartBase.Companion.AND
import com.lassanit.smotify.bases.SmartBase.Companion.EQUAL
import com.lassanit.smotify.bases.SmartBase.Companion.FROM
import com.lassanit.smotify.bases.SmartBase.Companion.IN
import com.lassanit.smotify.bases.SmartBase.Companion.SELECT
import com.lassanit.smotify.bases.SmartBase.Companion.SET
import com.lassanit.smotify.bases.SmartBase.Companion.UPDATE
import com.lassanit.smotify.bases.SmartBase.Companion.WHERE
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.display.data.AppMessage
import com.lassanit.smotify.display.data.SmartMedia
import com.lassanit.smotify.interfaces.ExternalImps
import java.io.File
import java.io.FileOutputStream
import java.util.Date


class EditBaseHelper(
    private val context: Context,
    private val sql: SQLiteOpenHelper,
    private val dbApp: AppBase,
    private val dbAppMessage: AppMessageBase,
    private val dbAppMedia: AppMediaBase
) : AppEditImp {
    companion object {
        fun get(context: Context): EditBaseHelper {
            return SmartBase(context).editHelper
        }
    }

    object Params {
        /**
         * @param visibility app vibility level
         * @sample SharedBase.Visibility.NORMAL
         * @sample SharedBase.Visibility.SECRET
         * @return query to update the list of app notifications/messages unread status where it is true
         * */
        fun getQueryAppMessagesRead(visibility: Int): String {
            return buildString {
                append(UPDATE).append(AppMessageBase.Params.TABLE).append(' ')
                append(SET).append(AppMessageBase.Params.KEY_IS_UNREAD).append(EQUAL).append(0)
                    .append(' ')
                append(WHERE).append(buildString {
                    append(AppMessageBase.Params.KEY_PKG).append(' ').append(IN)
                    append('(')
                        .append(buildString {
                            append(SELECT).append(AppBase.Params.KEY_PKG).append(' ')
                            append(FROM).append(AppBase.Params.TABLE).append(' ')
                            append(WHERE).append(AppBase.Params.KEY_VISIBILITY).append(EQUAL)
                                .append(visibility)
                        }).append(") ").append(AND)
                    append(AppMessageBase.Params.KEY_IS_UNREAD).append(EQUAL).append(1)
                })
            }
        }
    }

    fun getAppName(pkg: String): String? {
        return try {
            dbApp.getApp(pkg)?.name
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            e.printStackTrace()
            null
        }
    }

    fun addMessagingStyle(
        msg: AppMessage,
        sbnData: SbnData?,
        uri: Uri?,
        type: String?,
        map: Bitmap?,
        imp: ExternalImps.SmartPhoneServiceImp?
    ): Int {
        return runCatching {
            log("$msg, $uri, $type", "-sbn-saving")
            // ensuring app existence
            runCatching {
                if (dbApp.hasApp(msg.pkg) < 0) {
                    addApp(msg.pkg)
                }
            }
            val mediaName = if (uri != null) getMediaName(uri)
                ?: Date().time.toString() else if (map != null) Date().time.toString() else null

            //  msg.textDesc=
            val b = msg.textDesc ?: "".plus('(').plus(mediaName.toString()).plus(')')
                .replace("(null)", "")
                .replace("()", "")

            val od = msg.textDesc
            msg.textDesc = b
            var has = if (mediaName != null) {
                dbAppMessage.has(msg)
            } else {
                Pair(-1, false)
            }
            msg.textDesc = od

            if (has.first == -1) {
                has = dbAppMessage.add(msg)
            }

            val hasMedia = (uri != null).or(map != null)

            val mid = has.first
            SmartNotify.log("$mid, $hasMedia, ${msg.toString()}", "SERVICE")
            if (has.first > -1 && has.second.not() && hasMedia) {
                log("$type saving", "-sbn-saving")
                if (SharedBase.Settings.Display.isMediaSavingAllowed())
                    runCatching {
                        when {
                            uri == null -> {
                                runCatching {
                                    val media = SmartMedia(mid, null, map?.let { makeFile(it) })
                                    if (media.exist()) {
                                        if (dbAppMedia.addMedia(mid, media) >= 0) {
                                            if (dbAppMessage.updateMedia(
                                                    mid,
                                                    Date().time.toString()
                                                ).second
                                            )
                                                imp?.onMsgUpdated(msg.pkg, mid)
                                        }
                                    }
                                }
                            }

                            type == null -> {}

                            type.startsWith("application/") -> {

                            }

                            type.startsWith("image/") -> {
                                runCatching {
                                    //if (type.trim() == "image/*")
                                    getMedia(uri)
                                    val media = SmartMedia(mid, null, makeFile(uri, "jpg"))
                                    if (media.exist()) {
                                        if (dbAppMedia.addMedia(mid, media) >= 0)
                                            if (dbAppMessage.updateMediaState(
                                                    mid,
                                                    getMediaName(uri) ?: Date().time.toString()
                                                )
                                            )
                                                imp?.onMsgUpdated(msg.pkg, mid)
                                    }
                                }.onFailure { it.printStackTrace() }
                                //runCatching {
                                //    //val draw = Icon.createWithContentUri(uri).loadDrawable(context)
                                //    //if (draw != null) {
                                //    //    val media =
                                //    //        SmartMedia(mid, Resource.drawableToBitmap(draw), null)
                                //    //    if (media.exist()) {
                                //    //        if (dbAppMedia.addMedia(mid, media) >= 0)
                                //    //            if (dbAppMessage.updateMediaState(mid))
                                //    //                imp?.onMsgUpdated(msg.pkg, mid)
                                //    //    }
                                //    //}
                                //    val media = SmartMedia(mid, makeImageMap(uri), null)
                                //    if (media.exist()) {
                                //        if (dbAppMedia.addMedia(mid, media) >= 0)
                                //            if (dbAppMessage.updateMediaState(mid))
                                //                imp?.onMsgUpdated(msg.pkg, mid)
                                //    }
                                //}
                            }

                            type.startsWith("video/") -> {
                                runCatching {
                                    val media = SmartMedia(mid, null, makeFile(uri, "mp4"))
                                    if (media.exist()) {
                                        if (dbAppMedia.addMedia(mid, media) >= 0)
                                            if (dbAppMessage.updateMediaState(
                                                    mid,
                                                    getMediaName(uri) ?: Date().time.toString()
                                                )
                                            )
                                                imp?.onMsgUpdated(msg.pkg, mid)

                                    }
                                }
                            }

                            type.startsWith("audio/") -> {
                                runCatching {
                                    val media = SmartMedia(mid, null, makeFile(uri, "mp3"))
                                    if (media.exist()) {
                                        if (dbAppMedia.addMedia(mid, media) >= 0)
                                            if (dbAppMessage.updateMediaState(
                                                    mid,
                                                    getMediaName(uri) ?: Date().time.toString()
                                                )
                                            )
                                                imp?.onMsgUpdated(msg.pkg, mid)

                                    }
                                }
                            }

                            else -> {}
                        }
                    }.onFailure { it.printStackTrace() }
            }
            mid
        }
            .onFailure { it.printStackTrace();Firebase.crashlytics.recordException(it) }
            .getOrElse { -1 }
    }

    fun add(
        msg: AppMessage,
        sbnData: SbnData?,
        map: Bitmap?,
        imp: ExternalImps.SmartPhoneServiceImp?
    ): Int {
        return runCatching {
            // ensuring app existence
            runCatching {
                if (dbApp.hasApp(msg.pkg) < 0) {
                    addApp(msg.pkg)
                }
            }
            // adding message
            val mid = runCatching {
                dbAppMessage.addUniqueAppMessage(
                    msg,
                    sbnData
                ).first
            }.getOrElse { -1 }

            val hasMedia = (map != null)
            SmartNotify.log("$mid, $hasMedia, ${msg.toString()}", "SERVICE")

            if (mid >= 0) {
                runCatching {
                    if (SharedBase.Settings.Display.isMediaSavingAllowed()) {
                        val media = SmartMedia(mid, null, map?.let { makeFile(it) })
                        if (media.exist()) {
                            if (dbAppMedia.addMedia(mid, media) >= 0) {
                                if (dbAppMessage.updateMediaState(mid, Date().time.toString()))
                                    imp?.onMsgUpdated(msg.pkg, mid)
                            }
                        }
                    }
                }.onFailure { it.printStackTrace() }
            }
            mid
        }.onFailure { it.printStackTrace();Firebase.crashlytics.recordException(it) }
            .getOrElse { -1 }
    }

    fun add(
        msg: AppMessage,
        sbnData: SbnData?,
        uri: Uri?,
        type: String?,
        imp: ExternalImps.SmartPhoneServiceImp?
    ): Int {
        return runCatching {
            // ensuring app existence
            runCatching {
                if (dbApp.hasApp(msg.pkg) < 0) {
                    addApp(msg.pkg)
                }
            }
            // adding message
            val mid = runCatching {
                dbAppMessage.addUniqueAppMessage(
                    msg,
                    sbnData
                ).first
            }.getOrElse { -1 }
            runCatching {
                //UriReceiver.send(context, uri)
            }

            val hasMedia = (uri != null)
            SmartNotify.log("$mid, $hasMedia, ${msg.toString()}", "SERVICE")

            if (mid >= 0 && uri != null) {
                if (SharedBase.Settings.Display.isMediaSavingAllowed())
                    runCatching {
                        when {
                            type == null -> {}
                            type.startsWith("application/") -> {}

                            type.startsWith("image/") -> {
                                runCatching {
                                    val media = SmartMedia(mid, null, makeFile(uri, ".jpg"))
                                    if (media.exist()) {
                                        if (dbAppMedia.addMedia(mid, media) >= 0)
                                            if (dbAppMessage.updateMediaState(
                                                    mid,
                                                    getMediaName(uri) ?: Date().time.toString()
                                                )
                                            )
                                                imp?.onMsgUpdated(msg.pkg, mid)
                                    }
                                }.onFailure { it.printStackTrace() }
                                //runCatching {
                                //    //val draw = Icon.createWithContentUri(uri).loadDrawable(context)
                                //    //if (draw != null) {
                                //    //    val media =
                                //    //        SmartMedia(mid, Resource.drawableToBitmap(draw), null)
                                //    //    if (media.exist()) {
                                //    //        if (dbAppMedia.addMedia(mid, media) >= 0)
                                //    //            if (dbAppMessage.updateMediaState(mid))
                                //    //                imp?.onMsgUpdated(msg.pkg, mid)
                                //    //    }
                                //    //}
                                //    val media = SmartMedia(mid, makeImageMap(uri), null)
                                //    if (media.exist()) {
                                //        if (dbAppMedia.addMedia(mid, media) >= 0)
                                //            if (dbAppMessage.updateMediaState(mid))
                                //                imp?.onMsgUpdated(msg.pkg, mid)
                                //    }
                                //}
                            }

                            type.startsWith("video/") -> {
                                runCatching {
                                    val media = SmartMedia(mid, null, makeFile(uri, "mp4"))
                                    if (media.exist()) {
                                        if (dbAppMedia.addMedia(mid, media) >= 0)
                                            if (dbAppMessage.updateMediaState(
                                                    mid,
                                                    getMediaName(uri) ?: Date().time.toString()
                                                )
                                            )
                                                imp?.onMsgUpdated(msg.pkg, mid)

                                    }
                                }
                            }

                            type.startsWith("audio/") -> {
                                runCatching {
                                    val media = SmartMedia(mid, null, makeFile(uri, "mp3"))
                                    if (media.exist()) {
                                        if (dbAppMedia.addMedia(mid, media) >= 0)
                                            if (dbAppMessage.updateMediaState(
                                                    mid,
                                                    getMediaName(uri) ?: Date().time.toString()
                                                )
                                            )
                                                imp?.onMsgUpdated(msg.pkg, mid)

                                    }
                                }
                            }

                            else -> {}
                        }
                    }.onFailure { it.printStackTrace() }
            }
            mid
        }.onFailure { it.printStackTrace();Firebase.crashlytics.recordException(it) }
            .getOrElse { -1 }
    }

    private fun makeFile(map: Bitmap): Uri? {
        return runCatching {
            // Create a new file
            val file = File(
                context.filesDir,
                //Environment.getExternalStoragePublicDirectory(null),
                "${Date().time}.jpg"
            )
            val outputStream = FileOutputStream(file)
            map.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
            Uri.fromFile(file)
        }.getOrNull()
    }

    private fun makeFile(uri: Uri, ext: String): Uri? {
        return runCatching {
            val contentResolver: ContentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val data = ByteArray(inputStream!!.available())
            inputStream.read(data)
            inputStream.close()

            // Create a new file
            val file = File(
                context.filesDir,
                //Environment.getExternalStoragePublicDirectory(null),
                "${Date().time}.$ext"
            )
            val outputStream = FileOutputStream(file)
            outputStream.write(data)
            outputStream.close()

            Uri.fromFile(file)
        }.onFailure { it.printStackTrace() }.getOrNull()
    }

    fun getMediaName(uri: Uri): String? {
        return runCatching {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val name = cursor.getString(0)
                cursor.close()
                return name
            }
            null
        }.getOrNull()
    }

    fun getMedia(uri: Uri): Bitmap? {
        return runCatching {
            log("${uri.queryParameterNames}", "-sbn-img")


            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    log("${cursor.columnCount}, ${cursor.columnNames.toString()}", "-sbn-img")
                } while (cursor.moveToNext())

                cursor.close()
            }
            null
        }.onFailure { it.printStackTrace() }.getOrNull()
    }

    fun addCloudMessage(msg: AppMessage, key: String?, mid: String): Int {
        return add(msg, SbnData(key, mid), null, null)
    }

    private fun addCloudMedia(sbnData: SbnData, smartMedia: SmartMedia) {
        runCatching {
            val mid = dbAppMessage.getAppMessageId(sbnData)
            dbAppMessage.updateMediaState(mid, Date().time.toString())
            smartMedia.mid = mid
            dbAppMedia.addMedia(mid, smartMedia)
        }.onFailure { it.printStackTrace() }
    }

    fun addCloudMediaList(mediaList: ArrayList<SmartMedia>, key: String) {
        runCatching {
            for (media in mediaList) {
                addCloudMedia(SbnData(key, media.mid.toString()), media)
            }
        }.onFailure { it.printStackTrace() }
    }


    class SbnData(val tag: String?, val key: String?) {}

    /////////////////////////////////////////////////////////////////////////////////////////////////
    override fun addApp(app: App): Int {
        return dbApp.addApp(app)
    }

    override fun addApp(pkg: String): Int {
        return dbApp.addApp(pkg)
    }

    override fun addApp(pkg: String, name: String): Int {
        return dbApp.addApp(pkg, name)
    }

    override fun setVisibility(id: Int, visibility: Int): Boolean {
        return dbApp.setVisibility(id, visibility)
    }

    override fun setVisibility(pkg: String, visibility: Int): Boolean {
        return dbApp.setVisibility(pkg, visibility)
    }

    override fun setVisibility(pkg: String, name: String, visibility: Int): Boolean {
        return dbApp.setVisibility(pkg, name, visibility)
    }

///////////////////////////////////////////////////////////////////////////////////////////////

    fun readAppMessage(mid: Int): Boolean {
        return dbAppMessage.readAppMessage(mid)
    }

    fun readAppMessages(pkg: String): Boolean {
        return dbAppMessage.readAppMessages(pkg)
    }

    fun readAllAppMessages(): Boolean {
        return runCatching {
            val db = sql.writableDatabase;
            db.execSQL(Params.getQueryAppMessagesRead(SharedBase.Visibility.Phone.isNormalOrSecret()))
            db.close()
            true
        }.onFailure {
            it.printStackTrace()
        }.getOrElse { false }
    }

    private fun deleteMediaMessage(mid: Int): Boolean {
        return runCatching {
            val media = dbAppMedia.getMedia(mid) ?: return true
            if (media.getUri() != null) {
                runCatching {
                    File(
                        media.getUri().toString()
                    ).delete()
                }.onFailure { it.printStackTrace() }
            }
            val where = buildString {
                append(AppMediaBase.Params.KEY_MID).append(' ')
                append(EQUAL).append(mid)
            }
            val db = sql.writableDatabase
            val c = db.delete(AppMediaBase.Params.TABLE, where, null)
            db.close()
            return c > 0
        }.onFailure { it.printStackTrace() }.getOrElse { false }
    }

    private fun deleteMediaApp(pkg: String): Boolean {
        return runCatching {
            val msgIds: ArrayList<Int> = dbAppMessage.getMediaMessageIds(pkg)
            for (mid in msgIds) {
                deleteMediaMessage(mid)
            }
            true
        }.onFailure { it.printStackTrace() }.getOrElse { false }
    }

    fun deleteAppMessage(id: Int): Boolean {
        deleteMediaMessage(id)
        return dbAppMessage.deleteAppMessage(id)
    }

    fun deleteAppMessages(pkg: String): Boolean {
        deleteMediaApp(pkg)
        return dbAppMessage.deleteAppMessages(pkg)
    }


}