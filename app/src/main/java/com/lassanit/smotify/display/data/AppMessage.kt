package com.lassanit.smotify.display.data

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lassanit.smotify.classes.Resource.Companion.bitmapToByteArray
import com.lassanit.smotify.classes.Resource.Companion.byteArrayToBitmap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class AppMessage(
    val id: Int = -1,
    val type: Int,
    val pkg: String,
    val title: String,
    val titleDesc: String?,
    val text: String,
    var textDesc: String?,
    val time: Long,
    var isUnread: Boolean = true,
    val hasMedia: Boolean = false,
    val icon: Bitmap? = null
) {
    object Types {
        const val TYPE_MESSAGE = 1
        const val TYPE_INBOX = 2
        const val TYPE_BIG_PICTURE = 3
        const val TYPE_BIG_TEXT = 4
        const val TYPE_CALL = 5
        const val TYPE_DECORATE_CUSTOM = 6
        const val TYPE_CAR = 7
        const val TYPE_WEARABLE = 8
        const val TYPE_MEDIA = 9
        const val TYPE_NULL = -1
        const val TYPE_REPEAT = -2
    }

    companion object {
        fun toByteArray(appMessage: AppMessage): ByteArray {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val dataOutputStream = DataOutputStream(byteArrayOutputStream)

            dataOutputStream.writeInt(appMessage.id)
            dataOutputStream.writeInt(appMessage.type)
            dataOutputStream.writeUTF(appMessage.pkg)
            dataOutputStream.writeUTF(appMessage.title)
            dataOutputStream.writeUTF(appMessage.titleDesc ?: "")
            dataOutputStream.writeUTF(appMessage.text)
            dataOutputStream.writeUTF(appMessage.textDesc ?: "")
            dataOutputStream.writeLong(appMessage.time)
            dataOutputStream.writeBoolean(appMessage.isUnread)
            dataOutputStream.writeBoolean(appMessage.hasMedia)

            if (appMessage.icon != null) {
                val iconByteArray = bitmapToByteArray(appMessage.icon)
                dataOutputStream.writeInt(iconByteArray.size)
                dataOutputStream.write(iconByteArray)
            } else {
                dataOutputStream.writeInt(0)
            }

            dataOutputStream.close()
            return byteArrayOutputStream.toByteArray()
        }

        fun toAppMessage(byteArray: ByteArray): AppMessage {
            val dataInputStream = DataInputStream(ByteArrayInputStream(byteArray))

            val id = dataInputStream.readInt()
            val type = dataInputStream.readInt()
            val pkg = dataInputStream.readUTF()
            val title = dataInputStream.readUTF()
            val titleDesc = dataInputStream.readUTF()
            val text = dataInputStream.readUTF()
            val textDesc = dataInputStream.readUTF()
            val time = dataInputStream.readLong()
            val isUnread = dataInputStream.readBoolean()
            val hasMedia = dataInputStream.readBoolean()

            val icon: Bitmap?
            val iconSize = dataInputStream.readInt()
            if (iconSize > 0) {
                val iconByteArray = ByteArray(iconSize)
                dataInputStream.read(iconByteArray)
                icon = byteArrayToBitmap(iconByteArray)
            } else {
                icon = null
            }

            dataInputStream.close()
            return AppMessage(
                id,
                type,
                pkg,
                title,
                titleDesc.takeIf { it.isNotEmpty() },
                text,
                textDesc.takeIf { it.isNotEmpty() },
                time,
                isUnread,
                hasMedia,
                icon
            )
        }


    }

    override fun toString(): String {
        return "AppMessage(id=$id, type=$type, pkg='$pkg', title='$title', titleDesc=$titleDesc, text='$text', textDesc=$textDesc, time=$time, isUnread=$isUnread, hasMedia=$hasMedia, icon=$icon)"
    }

    fun toRoom(): RoomMessage {
        return RoomMessage(
            id,
            pkg,
            type,
            title,
            titleDesc,
            text,
            textDesc,
            time,
            isUnread,
            hasMedia,
            icon?.let { bitmapToByteArray(it) }
        )
    }

    @Entity
    data class RoomMessage(
        @PrimaryKey val mid: Int,
        @ColumnInfo(name = "messagePkg") val pkg: String,
        @ColumnInfo(name = "messageType") val type: Int,
        @ColumnInfo(name = "messageTitle") val title: String,
        @ColumnInfo(name = "messageTitleDesc") val titleDesc: String?,
        @ColumnInfo(name = "messageText") val text: String,
        @ColumnInfo(name = "messageTextDesc") val textDesc: String?,
        @ColumnInfo(name = "messageTime") val time: Long,
        @ColumnInfo(name = "messageIsUnread") val isUnread: Boolean,
        @ColumnInfo(name = "messageHasMedia") val hasMedia: Boolean,
        @ColumnInfo(name = "messageIcon") val icon: ByteArray?,
    ) {
        fun toAppMessage(): AppMessage? {
            return runCatching {
                AppMessage(
                    mid,
                    type,
                    pkg,
                    title,
                    titleDesc,
                    text,
                    textDesc,
                    time,
                    isUnread,
                    hasMedia,
                    icon?.let { byteArrayToBitmap(it) }
                )
            }.getOrNull()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RoomMessage

            if (mid != other.mid) return false
            if (pkg != other.pkg) return false
            if (type != other.type) return false
            if (title != other.title) return false
            if (titleDesc != other.titleDesc) return false
            if (text != other.text) return false
            if (textDesc != other.textDesc) return false
            if (time != other.time) return false
            if (isUnread != other.isUnread) return false
            if (hasMedia != other.hasMedia) return false
            if (icon != null) {
                if (other.icon == null) return false
                if (!icon.contentEquals(other.icon)) return false
            } else if (other.icon != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = mid
            result = 31 * result + pkg.hashCode()
            result = 31 * result + type
            result = 31 * result + title.hashCode()
            result = 31 * result + (titleDesc?.hashCode() ?: 0)
            result = 31 * result + text.hashCode()
            result = 31 * result + (textDesc?.hashCode() ?: 0)
            result = 31 * result + time.hashCode()
            result = 31 * result + isUnread.hashCode()
            result = 31 * result + hasMedia.hashCode()
            result = 31 * result + (icon?.contentHashCode() ?: 0)
            return result
        }

    }
}