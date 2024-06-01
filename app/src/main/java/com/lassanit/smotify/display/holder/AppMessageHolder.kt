package com.lassanit.smotify.display.holder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lassanit.smotify.R
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.display.adapter.AppMessageChatterAdapter
import com.lassanit.smotify.display.data.AppMessage
import com.lassanit.smotify.display.data.SmartMedia
import com.lassanit.smotify.display.view.SmartMediaView
import de.hdodenhof.circleimageview.CircleImageView

class AppMessageHolder(private val view: View) :
    RecyclerView.ViewHolder(view) {
    constructor(context: Context, viewGroup: ViewGroup?) : this(
        Companion.getView(context, viewGroup)
    )

    companion object {
        private fun getView(context: Context, viewGroup: ViewGroup?): View {
            return LayoutInflater.from(context)
                .inflate(R.layout.holder_row_app_message, viewGroup, false)
        }

        fun getHolder(context: Context, parent: ViewGroup): AppMessageHolder {
            return AppMessageHolder(
                DataHolder.inflate(
                    context,
                    parent,
                    R.layout.holder_row_app_message
                )
            )
        }
    }

    private val titleT: TextView = view.findViewById(R.id.holder_row_app_message_title)
    private val msgT: TextView = view.findViewById(R.id.holder_row_app_message_text)
    private val timeT: TextView = view.findViewById(R.id.holder_row_app_message_time)
    private val iconI: CircleImageView = view.findViewById(R.id.holder_row_app_message_icon)
    private val unreadI: ImageView = view.findViewById(R.id.holder_row_app_message_unread)
    private val mediaI: ImageView = view.findViewById(R.id.holder_row_app_message_ismedia)
    private val sMediaI: SmartMediaView = view.findViewById(R.id.holder_row_app_message_smart_media)

    var onItemClick: ((View, AppMessage, SmartMedia?) -> Unit)? = null
    var onItemLongClick: ((View, AppMessage, SmartMedia?) -> Unit)? = null
    var onItemMediaClick: ((View, AppMessage, SmartMedia?) -> Unit)? = null
    var onItemMediaLongClick: ((View, AppMessage, SmartMedia?) -> Unit)? = null
    var onItemIconClick: ((View, AppMessage) -> Unit)? = null


    fun handle(noti: AppMessage, smartMedia: SmartMedia?, quickGlance: Boolean): AppMessageHolder {
        titleT.text = noti.title
        msgT.text = noti.text
        if (quickGlance.not()) {
            msgT.maxLines = 1000
            view.setBackgroundResource(R.drawable.shape_background_app_home_pager)
        }
        timeT.text = Resource.getTimeOrDate(noti.time)
        Glide.with(view).asDrawable()
            .placeholder(com.lassanit.firekit.R.color.fireKit_opposite_theme).load(noti.icon)
            .into(iconI)
        unreadI.visibility = if (noti.isUnread) VISIBLE else GONE

        handleMedia(smartMedia, quickGlance)
        sMediaI.setOnClickListener {
            onItemMediaClick?.let { it(this.itemView, noti, smartMedia) }
        }
        sMediaI.setOnLongClickListener {
            onItemMediaLongClick?.let { it(this.itemView, noti, smartMedia) }
            true
        }
        iconI.setOnClickListener {
            onItemIconClick?.let { it(this.itemView, noti) }
        }
        this.view.setOnClickListener {
            onItemClick?.let { it(this.itemView, noti, smartMedia) }
        }
        this.view.setOnLongClickListener {
            onItemLongClick?.let { it(this.itemView, noti, smartMedia) }
            true
        }
        return this
    }

    fun handleBackground(type: AppMessageChatterAdapter.Chatter) {
        when (type) {
            AppMessageChatterAdapter.Chatter.START ->{view.setBackgroundResource(R.drawable.shape_chat_type_start)}
            AppMessageChatterAdapter.Chatter.CENTER -> {view.setBackgroundResource(R.drawable.shape_chat_type_center)}
            AppMessageChatterAdapter.Chatter.END -> {view.setBackgroundResource(R.drawable.shape_chat_type_end)}
            AppMessageChatterAdapter.Chatter.ALONE -> {view.setBackgroundResource(R.drawable.shape_chat_type_alone)}
        }
    }

    private fun setMediaIcon(res: Int?) {
        mediaI.visibility = if (res != null) {
            mediaI.setImageResource(res)
            VISIBLE
        } else {
            GONE
        }
    }

    private fun setMedia(show: Boolean, smartMedia: SmartMedia?) {
        sMediaI.visibility = if (show) {
            sMediaI.set(smartMedia)
            VISIBLE
        } else GONE
    }

    private fun handleMedia(smartMedia: SmartMedia?, quickGlance: Boolean) {
        runCatching {
            if (smartMedia == null) {
                setMedia(false, null)
                setMediaIcon(null)
            } else if (smartMedia.isAudio(view.context)) {
                setMedia(quickGlance.not(), smartMedia)
                setMediaIcon(R.drawable.base_audio)
            } else if (smartMedia.isVideo(view.context)) {
                setMedia(false, smartMedia)
                setMediaIcon(R.drawable.base_video)
            } else if (smartMedia.isImage(view.context)) {
                val showMedia = quickGlance.not()
                    .or(SharedBase.Settings.Display.isMediaShowInAppHeaderAllowed())
                setMedia(showMedia, smartMedia)
                setMediaIcon(R.drawable.base_img)
            }
        }
    }

    fun getView(): View {
        return view
    }
}