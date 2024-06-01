package com.lassanit.smotify.display.adapter

import android.content.Intent
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.bases.SmartBase
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.classes.SortWith
import com.lassanit.smotify.display.data.AppMessage
import com.lassanit.smotify.display.holder.AppMessageHolder
import com.lassanit.smotify.popups.AppDialog

interface AppMessageActions {
    fun readMessage(position: Int)
    fun readMessage(msg: AppMessage)
    fun readMessage(position: Int, msg: AppMessage)
    fun deleteMessage(position: Int)
    fun deleteMessage(msg: AppMessage)
    fun deleteMessage(position: Int, msg: AppMessage)
}

class AppMessageAdapter(
    val activity: FragmentActivity,
    val pkg: String,
    val db: SmartBase,
    private val callback: Callbacks?
) :
    RecyclerView.Adapter<AppMessageHolder>(), AppMessageActions {

    interface Callbacks {
        fun listDataUpdated(pkg: String)
    }

    private val list = ArrayList<AppMessage>()

    init {
        runCatching {
            list.clear()
            val list1 = db.displayHelper.getAppMessages(
                pkg,
                null,
                SharedBase.Settings.Display.getMinMessagesInAppHeader()
            )
            list.addAll(list1)
            list.sortWith(SortWith.AppMessage)

        }.onFailure { it.printStackTrace() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppMessageHolder {
        return AppMessageHolder.getHolder(activity, parent)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: AppMessageHolder, position: Int) {
        runCatching {
            val msg = list[position]
            holder.handle(msg, db.displayHelper.getMedia(msg.id), true)
            holder.onItemClick = { a, b, c ->
                runCatching {
                    if (SharedBase.Settings.Display.isMediaClickInAppHeaderAllowed()) {
                        AppDialog.needDisplay.make(a.context, b, c) { sharingView ->
                            Resource.shareViewAsImage(
                                sharingView, System.currentTimeMillis().toString()
                            ) { sharingIntent ->
                                if (sharingIntent != null)
                                    activity.startActivity(
                                        Intent.createChooser(sharingIntent, "Share as Image")
                                    )
                            }
                        }.show()
                        if (b.isUnread)
                            this.readMessage(b)
                    }
                }.onFailure { it.printStackTrace() }
            }
            holder.onItemLongClick = { a, b, c ->
                runCatching {
                    AppDialog.needMenu.make(
                        activity, a, this, b, c,
                        SharedBase.Visibility.Phone.isNormalNotifications()
                    ).show()
                }.onFailure { it.printStackTrace() }
            }
            holder.onItemIconClick = { _, _ -> }
            holder.onItemMediaClick = { _, _, c ->
                runCatching {
                    if (SharedBase.Settings.Display.isMediaClickInAppHeaderAllowed()) {
                        if (c?.isImage(activity) == true) {
                            AppDialog.needDisplay.make(activity, c)?.show()
                        }
                    }
                }.onFailure { it.printStackTrace() }
            }
            holder.onItemMediaLongClick = { _, _, _ -> }
        }.onFailure { it.printStackTrace() }
    }

    override fun readMessage(position: Int) {
        runCatching {
            readMessage(position, list[position])
        }.onFailure { it.printStackTrace() }
    }

    override fun readMessage(msg: AppMessage) {
        runCatching { readMessage(list.indexOf(msg), msg) }.onFailure { it.printStackTrace() }
    }

    override fun readMessage(position: Int, msg: AppMessage) {
        runCatching {
            if (db.editHelper.readAppMessage(msg.id)) {
                list[position].isUnread = false
                notifyItemChanged(position)
                callback?.listDataUpdated(pkg)
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun deleteMessage(position: Int) {
        runCatching {
            deleteMessage(position, list[position])
        }.onFailure { it.printStackTrace() }
    }

    override fun deleteMessage(msg: AppMessage) {
        runCatching { deleteMessage(list.indexOf(msg), msg) }.onFailure { it.printStackTrace() }
    }

    override fun deleteMessage(position: Int, msg: AppMessage) {
        runCatching {
            if (db.editHelper.deleteAppMessage(msg.id)) {
                list.removeAt(position)
                notifyItemRemoved(position)
                callback?.listDataUpdated(pkg)
            }
        }.onFailure { it.printStackTrace() }
    }


}