package com.lassanit.smotify.display.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

class SmartMedia(var mid: Int, private val map: Bitmap?, private val uri: Uri?) {
    fun exist(): Boolean {
        return (map != null).or(uri != null)
    }

    fun isImage(context: Context): Boolean {
        return runCatching {
            if (map != null)
                return true
            uri?.let {
                val type = context.contentResolver.getType(it)
                    ?: return uri.toString().endsWith(".jpg", true)
                        .or(uri.toString().endsWith(".jpeg",true))
                        .or(uri.toString().endsWith(".png",true))
                        .or(uri.toString().endsWith(".gif",true))
                        .or(uri.toString().endsWith(".bmp",true))
                return type.startsWith("image/", true)
            }
            return false
        }.getOrElse { false }
    }

    fun isAudio(context: Context): Boolean {
        return runCatching {
            uri?.let {
                val type = context.contentResolver.getType(it)
                    ?: return uri.toString().endsWith(".aac", true)
                        .or(uri.toString().endsWith(".mp3",true))
                        .or(uri.toString().endsWith(".acc",true))
                        .or(uri.toString().endsWith(".ogg",true))
                        .or(uri.toString().endsWith(".mpeg",true))
                        .or(uri.toString().endsWith(".wav",true))
                return type.startsWith("audio/", true)
            }
            return false
        }.getOrElse { false }
    }

    fun isVideo(context: Context): Boolean {
        return runCatching {
            uri?.let {
                val type = context.contentResolver.getType(it)
                    ?: return uri.toString().endsWith(".mp4", true)
                        .or(uri.toString().endsWith(".3gpp",true))
                        .or(uri.toString().endsWith(".3gpp2",true))
                        .or(uri.toString().endsWith(".webm",true))
                return type.startsWith("video/", true)
            }
            return false
        }.getOrElse { false }
    }

    fun getMap(): Bitmap? {
        return map
    }

    fun getUri(): Uri? {
        return uri
    }
}