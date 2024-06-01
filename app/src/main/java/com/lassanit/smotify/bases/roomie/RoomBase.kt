package com.lassanit.smotify.bases.roomie

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.display.data.AppHeader
import com.lassanit.smotify.display.data.AppMessage

@Database(
    entities = [App.RoomApp::class, AppHeader.RoomHeader::class, AppMessage.RoomMessage::class],
    version = 1
)
abstract class RoomBase : RoomDatabase() {
    companion object {
        fun build(context: Context): RoomBase {
            return Room.databaseBuilder(context, RoomBase::class.java, "smart-notify-room-1")
                .build()
        }
    }

    abstract fun app(): ViewerDao.AppDao
    abstract fun headers(): ViewerDao.AppHeaderDao
    abstract fun messages(): ViewerDao.MessageDao
}