package com.lassanit.smotify.display.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lassanit.smotify.bases.SharedBase

class App(
    var pkg: String,
    var name: String,
    var visibility: Int = SharedBase.Visibility.NORMAL,
    var id: Int = -1
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pkg)
        parcel.writeString(name)
        parcel.writeInt(visibility)
        parcel.writeInt(id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<App> {
        override fun createFromParcel(parcel: Parcel): App {
            return App(parcel)
        }

        override fun newArray(size: Int): Array<App?> {
            return arrayOfNulls(size)
        }
    }

    fun toRoom(): RoomApp {
        return RoomApp(id, pkg, name, visibility)
    }

    @Entity(indices = [Index(value = ["appPkg"], unique = true)])
    data class RoomApp(
        @PrimaryKey val appId: Int,
        @ColumnInfo(name = "appPkg") val pkg: String,
        @ColumnInfo(name = "appName") val name: String,
        @ColumnInfo(name = "appVisibility") val visibility: Int = SharedBase.Visibility.NORMAL,
    ) {
        fun toApp(): App? {
            return runCatching { App(pkg, name, visibility, appId) }.getOrNull()
        }
    }

}
