package com.lassanit.smotify.services_receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toIcon
import androidx.core.net.toFile
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lassanit.smotify.SmartNotify


@SuppressLint("NewApi")
class UriReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG="UriReceiver"
        fun send(context: Context, uri: Uri) {
            runCatching {
                val manager = LocalBroadcastManager.getInstance(context)
                manager.registerReceiver(
                    UriReceiver(),
                    IntentFilter.create("android.intent.action.SEND_ABC", "*/*")
                )
                manager.sendBroadcastSync(
                    Intent("android.intent.action.SEND_ABC").setData(uri)
                )
            }
        }
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        runCatching {
            IconCompat.TYPE_RESOURCE
            val uri = p1?.data ?: return@runCatching
            SmartNotify.log("1::runCatch()-> :: $uri", TAG)
            SmartNotify.log("1::runCatch()-> toIcon:: ${uri.toIcon()}", TAG)
            SmartNotify.log("1::runCatch()-> toIcon.type:: ${uri.toIcon().type}", TAG)
            SmartNotify.log("1::runCatch()-> toIcon.uri:: ${uri.toIcon().uri}", TAG)
        }
        runCatching {
            val uri = p1?.data ?: return@runCatching
            SmartNotify.log("2::runCatch()-> :: $uri", TAG)
            SmartNotify.log("2::runCatch()-> toFile:: ${uri.toFile()}", TAG)
            SmartNotify.log("2::runCatch()-> toFile.name:: ${uri.toFile().name}", TAG)
            SmartNotify.log("2::runCatch()-> toFile.path:: ${uri.toFile().path}", TAG)
        }

        runCatching {
            val uri = p1?.data ?: return@runCatching
            val cursor: Cursor? = p0?.contentResolver?.query(uri, null, null, null, null)
            runCatching {
                SmartNotify.log("\t3.1:runCatch()-> count:: ${cursor?.count}, ${cursor?.columnCount}", TAG)
                if (cursor != null && cursor.moveToFirst()) {
                    val name=cursor.getString(0)
                    val size=cursor.getLong(1)
                    SmartNotify.log("3.1::runCatch()-> name:: $name, $size", TAG)
                    cursor.close()
                    //checkData(p0,uri.authority.toString(),name)
                }
            }
        }
    }


    /*

    private fun getUri(paramContext: Context, paramFile: File): Uri? {
        return FileProvider.getUriForFile(
            paramContext,
            "com.lassanit.smotify.provider",
            paramFile
        )
    }


    fun getUri(paramContext: Context, paramString: String): Uri? {
        return FileProvider.getUriForFile(
            paramContext,
            buildString { append(paramContext.packageName).append(".provider") },
            File(paramString)
        )
    }
    fun getMimeType(paramUri: Uri): String? {
        var str: String?
        if (paramUri.scheme == "content") {
            str = getContentResolver().getType(paramUri)
        } else {
            str = MimeTypeMap.getFileExtensionFromUrl(str.toString())
            str = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(str.lowercase(Locale.getDefault()))
        }
        val stringBuilder = StringBuilder()
        stringBuilder.append("")
        stringBuilder.append(str)
        s.LOGI("MimeType", stringBuilder.toString())
        return str
    }

    fun getMimeType(paramString: String?): String? {
        var paramString = paramString
        paramString = MimeTypeMap.getFileExtensionFromUrl(paramString)
        paramString = if (paramString != null) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(paramString)
        } else {
            null
        }
        return paramString
    }
   */
}