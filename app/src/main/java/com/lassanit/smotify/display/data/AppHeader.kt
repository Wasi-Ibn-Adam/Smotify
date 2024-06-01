package com.lassanit.smotify.display.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

class AppHeader(
    val pkg: String,
    val pkgName: String,
    val visibility: Int,
    val total: Long,
    val unread: Long,
    val time: Long
) {

    @Entity
    data class RoomHeader(
        @PrimaryKey
        @ColumnInfo(name = "headerPkg") val pkg: String,
        @ColumnInfo(name = "headerPkgName") val pkgName: String,
        @ColumnInfo(name = "headerVisibility") val visibility: Int,
        @ColumnInfo(name = "headerTotal") val total: Long,
        @ColumnInfo(name = "headerUnread") val unread: Long,
        @ColumnInfo(name = "headerTime") val time: Long
    ) {
        fun toAppHeader(): AppHeader? {
            return runCatching {
                AppHeader(
                    pkg,
                    pkgName,
                    visibility,
                    total,
                    unread,
                    time
                )
            }.getOrNull()
        }
    }

}