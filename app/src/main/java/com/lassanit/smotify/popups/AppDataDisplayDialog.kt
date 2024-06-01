package com.lassanit.smotify.popups

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatButton
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.lassanit.smotify.R
import com.lassanit.smotify.R.drawable
import com.lassanit.smotify.R.id
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.display.data.AppData
import com.lassanit.smotify.display.data.AppHeader
import com.lassanit.smotify.display.data.AppMessage
import com.lassanit.smotify.display.data.SmartMedia
import com.lassanit.smotify.display.view.SmartMediaView
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Date

class AppDataDisplayDialog(context: Context, private val type: Int) : AppDialog(context) {
    object Type {
        const val TYPE_APP = 10100
        const val TYPE_HEADER = 10101
        const val TYPE_MESSAGE = 10102
        const val TYPE_AUDIO = 20100
        const val TYPE_IMAGE = 20101
        const val TYPE_IMAGE_CIRCLE = 20102
        const val TYPE_LAYOUT_RES = 30101
        const val TYPE_LAYOUT_VIEW = 30102
        const val TYPE_LAYOUT_WELCOME = 30103
    }

    private lateinit var logoI: CircleImageView
    private lateinit var nameT: TextView
    private lateinit var pkgT: TextView
    private lateinit var countT: TextView
    private lateinit var totalT: TextView
    private lateinit var timeT: TextView

    private lateinit var titleT: TextView
    private lateinit var titleDescT: TextView
    private lateinit var textT: TextView
    private lateinit var textDescT: TextView

    private lateinit var simgI: ShapeableImageView

    private lateinit var btnB: AppCompatButton

    private lateinit var appNameT: TextView
    private lateinit var appPkgT: TextView
    private lateinit var appVersionT: TextView
    private lateinit var appStatusT: TextView
    private lateinit var appInstallT: TextView
    private lateinit var appUpdateT: TextView
    private lateinit var appLinkT: TextView
    private lateinit var smartSM: SmartMediaView

    private lateinit var shareMessage: ImageView


    init {
        runCatching {
            when (type) {
                Type.TYPE_HEADER -> {
                    setContentView(R.layout.dialog_appdata_display_header)
                    logoI = findViewById(id.dialog_appdata_display_header_logo)
                    nameT = findViewById(id.dialog_appdata_display_header_name)
                    pkgT = findViewById(id.dialog_appdata_display_header_pkg)
                    totalT = findViewById(id.dialog_appdata_display_header_total)
                    countT = findViewById(id.dialog_appdata_display_header_unread)
                    timeT = findViewById(id.dialog_appdata_display_header_time)
                }

                Type.TYPE_MESSAGE -> {
                    setContentView(R.layout.dialog_appdata_display_message)
                    logoI = findViewById(id.dialog_appdata_display_message_icon)
                    titleT = findViewById(id.dialog_appdata_display_message_title)
                    titleDescT = findViewById(id.dialog_appdata_display_message_subtitle)
                    textT = findViewById(id.dialog_appdata_display_message_text)
                    textDescT = findViewById(id.dialog_appdata_display_message_subtext)
                    timeT = findViewById(id.dialog_appdata_display_message_time)
                    smartSM = findViewById(id.dialog_appdata_display_message_media)
                    shareMessage = findViewById(id.dialog_appdata_display_message_share)
                }

                Type.TYPE_AUDIO -> {
                    setContentView(R.layout.dialog_appdata_display_audio)
                    smartSM = findViewById(id.dialog_appdata_display_audio_media)
                }

                Type.TYPE_IMAGE -> {
                    setContentView(R.layout.dialog_appdata_display_image)
                    simgI = findViewById(id.dialog_appdata_display_image_img)
                }

                Type.TYPE_IMAGE_CIRCLE -> {
                    setContentView(R.layout.dialog_appdata_display_image)
                    logoI = findViewById(id.dialog_appdata_display_image_circle)
                }

                Type.TYPE_LAYOUT_WELCOME -> {
                    setContentView(R.layout.dialog_appdata_display_welcome)
                    titleT = findViewById(id.dialog_appdata_display_welcome_title)
                    titleDescT = findViewById(id.dialog_appdata_display_welcome_text_1)
                    textT = findViewById(id.dialog_appdata_display_welcome_text_2)
                    textDescT = findViewById(id.dialog_appdata_display_welcome_text_3)
                    btnB = findViewById(id.dialog_appdata_display_welcome_btn)

                    titleT.setText(R.string.welcome_title)
                    titleDescT.text = HtmlCompat.fromHtml(
                        context.getString(R.string.welcome_text_1),
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                    textT.text = HtmlCompat.fromHtml(
                        context.getString(R.string.welcome_text_2),
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                    textDescT.text = HtmlCompat.fromHtml(
                        context.getString(R.string.welcome_text_3),
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )

                    btnB.setOnClickListener { dismiss() }
                }

                Type.TYPE_APP -> {
                    setContentView(R.layout.dialog_appdata_display_app)
                    logoI = findViewById(id.dialog_appdata_display_app_logo)
                    appNameT = findViewById(id.dialog_appdata_display_app_name)
                    appPkgT = findViewById(id.dialog_appdata_display_app_pkg)
                    appStatusT = findViewById(id.dialog_appdata_display_app_status)
                    appInstallT = findViewById(id.dialog_appdata_display_app_install)
                    appUpdateT = findViewById(id.dialog_appdata_display_app_update)
                    appVersionT = findViewById(id.dialog_appdata_display_app_version)
                    appLinkT = findViewById(id.dialog_appdata_display_app_link)
                }

                Type.TYPE_LAYOUT_RES, Type.TYPE_LAYOUT_VIEW -> {

                }

                else -> {
                    throw Exception("Invalid Type of AppDataDialog")
                }
            }

            initFinalize(true)
        }
    }

    private fun set(data: AppData): AppDataDisplayDialog {
        runCatching {
            appNameT.text = data.app.name
            appPkgT.text = data.app.pkg
            appStatusT.text = (if (data.active) "ON" else "OFF")
            appLinkT.text = Resource.storeLink(data.app.pkg)
            appInstallT.text =
                if (data.install != 0L) Date(data.install).toString() else " App Removed"
            appUpdateT.text = if (data.update != 0L) Date(data.update).toString() else ""
            appVersionT.text = data.version
            val draw = try {
                context.packageManager.getApplicationIcon(data.app.pkg)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            Glide.with(context).asDrawable().load(draw).placeholder(drawable.base_removed)
                .into(logoI)
        }
        return this
    }

    private fun set(data: AppHeader): AppDataDisplayDialog {
        runCatching {
            nameT.text = data.pkgName
            pkgT.text = data.pkg
            totalT.text = "${data.total}"
            countT.text = "${data.unread}"
            timeT.text = Date(data.time).toString()

            val draw = try {
                context.packageManager.getApplicationIcon(data.pkg)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            Glide.with(context).asDrawable().load(draw).placeholder(drawable.android).into(logoI)
        }
        return this
    }

    private fun set(
        data: AppMessage,
        media: SmartMedia?,
        onShare: (View) -> Unit
    ): AppDataDisplayDialog {
        runCatching {
            titleT.text = data.title
            titleDescT.text = data.titleDesc
            textT.text = data.text
            textDescT.text = data.textDesc
            timeT.text = Date(data.time).toString()
            Glide.with(context).load(data.icon).placeholder(drawable.base_removed).into(logoI)
            smartSM.set(media)
            shareMessage.setOnClickListener {
                runCatching {
                    onShare(findViewById(id.dialog_appdata_display_message))
                }.onFailure { it.printStackTrace() }
            }
        }
        return this
    }

    private fun set(data: Bitmap): AppDataDisplayDialog {
        runCatching {
            when (type) {
                Type.TYPE_IMAGE -> Glide.with(context).load(data).into(simgI)
                Type.TYPE_IMAGE_CIRCLE -> Glide.with(context).load(data).into(logoI)
                else -> {}
            }
        }
        return this
    }

    private fun set(data: Drawable): AppDataDisplayDialog {
        runCatching {
            when (type) {
                Type.TYPE_IMAGE -> Glide.with(context).load(data).into(simgI)
                Type.TYPE_IMAGE_CIRCLE -> Glide.with(context).load(data).into(logoI)
                else -> {}
            }
        }
        return this
    }

    private fun set(data: Uri): AppDataDisplayDialog {
        runCatching {
            when (type) {
                Type.TYPE_IMAGE -> Glide.with(context).load(data).into(simgI)
                Type.TYPE_IMAGE_CIRCLE -> Glide.with(context).load(data).into(logoI)
                Type.TYPE_AUDIO -> smartSM.set(SmartMedia(-1, null, data))
                else -> {}
            }
        }
        return this
    }

    private fun set(@LayoutRes layout: Int): AppDataDisplayDialog {
        runCatching {
            when (type) {
                Type.TYPE_LAYOUT_RES -> setContentView(layout)
            }
        }
        return this
    }

    private fun set(view: View): AppDataDisplayDialog {
        runCatching {
            when (type) {
                Type.TYPE_LAYOUT_VIEW -> setContentView(view)
            }
        }
        return this
    }

    fun onDismiss(runnable: Runnable): AppDataDisplayDialog {
        runCatching {
            setOnDismissListener {
                runnable.run()
            }
        }
        return this
    }

    companion object {
        fun make(context: Context, data: AppHeader): AppDataDisplayDialog {
            return AppDataDisplayDialog(context, Type.TYPE_HEADER).set(data)
        }

        fun make(
            context: Context,
            data: AppMessage,
            media: SmartMedia?,
            onShare: ((View) -> Unit)
        ): AppDataDisplayDialog {
            return AppDataDisplayDialog(context, Type.TYPE_MESSAGE).set(data, media, onShare)
        }

        fun make(context: Context, data: Uri, isCircle: Boolean = false): AppDataDisplayDialog {
            return AppDataDisplayDialog(
                context, if (isCircle) Type.TYPE_IMAGE_CIRCLE else Type.TYPE_IMAGE
            ).set(data)
        }

        fun make(context: Context, data: Bitmap, isCircle: Boolean = false): AppDataDisplayDialog {
            return AppDataDisplayDialog(
                context, if (isCircle) Type.TYPE_IMAGE_CIRCLE else Type.TYPE_IMAGE
            ).set(data)
        }

        fun make(
            context: Context, data: Drawable, isCircle: Boolean = false
        ): AppDataDisplayDialog {
            return AppDataDisplayDialog(
                context, if (isCircle) Type.TYPE_IMAGE_CIRCLE else Type.TYPE_IMAGE
            ).set(data)
        }

        fun make(context: Context, @LayoutRes data: Int): AppDataDisplayDialog {
            return AppDataDisplayDialog(context, Type.TYPE_LAYOUT_RES).set(data)
        }

        fun make(context: Context, data: View): AppDataDisplayDialog {
            return AppDataDisplayDialog(context, Type.TYPE_LAYOUT_VIEW).set(data)
        }

        fun make(context: Context, data: AppData): AppDataDisplayDialog {
            return AppDataDisplayDialog(context, Type.TYPE_APP).set(data)
        }

        fun make(context: Context, data: Uri): AppDataDisplayDialog {
            return AppDataDisplayDialog(context, Type.TYPE_AUDIO).set(data)
        }

        fun make(context: Context, data: SmartMedia): AppDataDisplayDialog? {
            if (data.exist().not()) return null
            if (data.isImage(context)) {
                return if (data.getMap() != null)
                    AppDataDisplayDialog(context, Type.TYPE_IMAGE).set(data.getMap()!!)
                else
                    AppDataDisplayDialog(context, Type.TYPE_IMAGE).set(data.getUri()!!)
            }
            if (data.isAudio(context))
                return AppDataDisplayDialog(context, Type.TYPE_AUDIO).set(data.getUri()!!)
            return null
        }

        fun makeHowToUse(context: Context): AppDataDisplayDialog {
            return AppDataDisplayDialog(
                context, Type.TYPE_LAYOUT_RES
            ).set(R.layout.dialog_appdata_display_how_to_use)
        }

        fun makeWelcome(context: Context): AppDataDisplayDialog {
            return AppDataDisplayDialog(
                context, Type.TYPE_LAYOUT_WELCOME
            )
        }


    }

}