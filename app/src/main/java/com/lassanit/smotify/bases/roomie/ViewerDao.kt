package com.lassanit.smotify.bases.roomie

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.display.data.AppHeader
import com.lassanit.smotify.display.data.AppMessage
import kotlinx.coroutines.flow.Flow

class ViewerDao {

    @Dao
    interface AppDao {
        @Query("SELECT * FROM roomapp")
        fun getAll(): List<App.RoomApp>

        @Query("SELECT * FROM roomapp WHERE appName=:name LIMIT 1")
        fun findByName(name: String): App.RoomApp

        @Query("SELECT * FROM roomapp WHERE appPkg=:pkg LIMIT 1")
        fun findByPackage(pkg: String): App.RoomApp

        @Query("SELECT appName FROM roomapp WHERE appPkg=:pkg LIMIT 1")
        fun findNameByPackage(pkg: String): String?

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        fun insertAll(vararg apps: App.RoomApp)

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        fun insert(app: App.RoomApp): Long

        @Query("UPDATE roomapp SET appVisibility = :visibility WHERE appId = :id ")
        fun update(id: Int, visibility: Int): Int

        @Query("UPDATE roomapp SET appVisibility = :visibility WHERE appPkg = :pkg ")
        fun update(pkg: String, visibility: Int): Int

        @Query("UPDATE roomapp SET appVisibility = NOT appVisibility WHERE appPkg = :pkg ")
        fun toggleVisibility(pkg: String): Int

        @Delete
        fun delete(apps: App.RoomApp): Int
    }

    @Dao
    interface AppHeaderDao {
        @Query(
            "SELECT appPkg AS headerPkg, " +
                    "appName AS headerPkgName, " +
                    "appVisibility AS headerVisibility, " +
                    "COUNT(mid) AS headerTotal, " +
                    "SUM(messageIsUnread) AS headerUnread, " +
                    "MAX(messageTime) AS headerTime " +
                    "FROM roomapp, roommessage " +
                    "WHERE appPkg =messagePkg AND appVisibility=:visibility " +
                    "LIMIT 1"
        )
        fun loadLiveAppHeaders(visibility: Int = SharedBase.Visibility.Phone.isNormalOrSecret()): LiveData<List<AppHeader.RoomHeader>>

        @Query(
            "SELECT appPkg AS headerPkg, " +
                    "appName AS headerPkgName, " +
                    "appVisibility AS headerVisibility, " +
                    "COUNT(mid) AS headerTotal, " +
                    "SUM(messageIsUnread) AS headerUnread, " +
                    "MAX(messageTime) AS headerTime " +
                    "FROM roomapp, roommessage " +
                    "WHERE appPkg =messagePkg AND appVisibility=:visibility " +
                    "LIMIT 1"
        )
        fun loadAppHeaders(visibility: Int = SharedBase.Visibility.Phone.isNormalOrSecret()): Flow<List<AppHeader.RoomHeader>>

        @Query(
            "SELECT appPkg AS headerPkg, " +
                    "appName AS headerPkgName, " +
                    "appVisibility AS headerVisibility, " +
                    "COUNT(mid) AS headerTotal, " +
                    "SUM(messageIsUnread) AS headerUnread, " +
                    "MAX(messageTime) AS headerTime " +
                    "FROM roomapp, roommessage " +
                    "WHERE appPkg =messagePkg AND appPkg=:pkg"
        )
        fun loadAppHeader(pkg: String): Flow<AppHeader.RoomHeader?>

    }

    @Dao
    interface MessageDao {
        @Query("SELECT * FROM roommessage WHERE messageTime >=:since ORDER BY messageTime ASC LIMIT :max")
        fun getAllAsc(since: Long = 0L, max: Int): List<AppMessage.RoomMessage>

        @Query("SELECT * FROM roommessage WHERE messageTime >=:since ORDER BY messageTime ASC")
        fun getAllAsc(since: Long = 0L): List<AppMessage.RoomMessage>

        @Query("SELECT * FROM roommessage WHERE messageTime >=:since ORDER BY messageTime DESC LIMIT :max")
        fun getAllDesc(since: Long = 0L, max: Int): List<AppMessage.RoomMessage>

        @Query("SELECT * FROM roommessage WHERE messageTime >=:since ORDER BY messageTime DESC")
        fun getAllDesc(since: Long = 0L): List<AppMessage.RoomMessage>


        @Query("UPDATE roommessage SET messageIsUnread=0")
        fun readAllAppMessages(): Int

        @Query("UPDATE roommessage SET messageIsUnread=0 WHERE mid=:id")
        fun readAppMessage(id: Int): Int

        @Query("UPDATE roommessage SET messageIsUnread=0 WHERE messagePkg=:pkg")
        fun readAppMessages(pkg: String): Int

        @Query("UPDATE roommessage SET messageIsUnread=0 WHERE messagePkg=:pkg AND messageTitle=:title")
        fun readAppMessages(pkg: String, title: String): Int



        @Query("SELECT COUNT(mid) FROM roommessage WHERE messageIsUnread =1")
        fun getUnreadAppMessagesCount(): Long

        @Query("SELECT COUNT(mid) FROM roommessage WHERE messageIsUnread =1 AND messagePkg=:pkg")
        fun getUnreadAppMessagesCount(pkg: String): Long


        @Query("UPDATE roommessage SET messageHasMedia=1 AND messageTextDesc=messageTextDesc+(:mediaName) WHERE mid=:mid")
        fun updateMedia(mid: Int, mediaName: String): Int


        @Insert(onConflict = OnConflictStrategy.IGNORE)
        fun insertAll(vararg apps: AppMessage.RoomMessage)

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        fun insert(app: AppMessage.RoomMessage): Long

        @Query("DELETE FROM roommessage WHERE mid=:id")
        fun delete(id: Int): Int

        @Query("DELETE FROM roommessage WHERE messagePkg=:pkg")
        fun delete(pkg: String): Int

        @Query("DELETE FROM roommessage WHERE messagePkg=:pkg AND messageTitle=:title")
        fun delete(pkg: String, title: String): Int

        @Delete
        fun delete(apps: AppMessage.RoomMessage): Int
    }

}