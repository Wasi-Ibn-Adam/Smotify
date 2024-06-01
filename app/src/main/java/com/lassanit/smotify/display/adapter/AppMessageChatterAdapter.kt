package com.lassanit.smotify.display.adapter

import android.content.Intent
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.bases.SmartBase
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.classes.SortWith
import com.lassanit.smotify.display.data.AppMessage
import com.lassanit.smotify.display.holder.AppMessageHolder
import com.lassanit.smotify.popups.AppDialog


class AppMessageChatterAdapter(
    val activity: FragmentActivity,
    val pkg: String,
    val db: SmartBase,
    private val callback: Callbacks?
) :
    RecyclerView.Adapter<AppMessageHolder>(), AppMessageActions {

    interface Callbacks {
        fun listDataUpdated(pkg: String)
    }

    enum class Chatter {
        START, CENTER, END, ALONE
    }

    var onScrollRequired: ((Int) -> Unit)? = null
    private val list = ArrayList<Pair<Chatter, AppMessage>>()

    init {
        runCatching {
            val list = db.displayHelper.getAppMessages(pkg, null, -1)
            convertChatter(list, false)
        }.onFailure { it.printStackTrace() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppMessageHolder {
        return AppMessageHolder.getHolder(activity, parent)
    }

    override fun onBindViewHolder(holder: AppMessageHolder, position: Int) {
        runCatching {
            val msg = list[position].second
            holder.handle(msg, db.displayHelper.getMedia(msg.id), false)
            holder.handleBackground(list[position].first)
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

    override fun getItemCount(): Int {
        return list.size
    }

    fun reload() {
        runCatching {
            list.clear()
            convertChatter(db.displayHelper.getAppMessages(pkg), true)
        }
    }

    fun messageAddedListUpdate() {
        runCatching {
            val msg = list.firstOrNull()?.second
            SmartNotify.log(msg.toString(), "Chatter")
            convertChatter(
                db.displayHelper.getAppMessages(pkg, msg, false),
                true
            )
        }
    }

    fun messageUpdatedUpdateList(mid: Int) {
        runCatching {
            val index = indexOf(mid)
            if (index != -1) {
                val oldPair = list[index]
                val msg = db.displayHelper.getAppMessage(mid) ?: return
                list[index] = Pair(oldPair.first, msg)
                notifyItemChanged(index)
            }
        }
    }

    private fun convertChatter(list: ArrayList<AppMessage>, addedLater: Boolean) {
        runCatching {
            val subList = ArrayList<AppMessage>()
            for (msg in list) {
                if (subList.isEmpty()) {
                    subList.add(msg)
                    continue
                }
                val title = subList.last().title
                if (title == msg.title) {
                    subList.add(msg)
                    continue
                } else {
                    SmartNotify.log("Title: $title -${subList.size}", "Chatter")
                    handleChatter(subList, addedLater)
                    subList.clear()
                    subList.add(msg)
                }
            }
            if (subList.isNotEmpty()) {
                SmartNotify.log("Title: ${subList.first().title} -${subList.size}", "Chatter")
                handleChatter(subList, addedLater)
            }
        }.onFailure { it.printStackTrace() }
    }

    private fun handleChatter(subList: ArrayList<AppMessage>, addedLater: Boolean) {
        runCatching {
            subList.sortWith(SortWith.AppMessage)
            if (addedLater) {
                val last = list.firstOrNull()
                if (last != null) {
                    if (last.second.title == subList.last().title) {
                        SmartNotify.log("${last.second.title}, ${subList.last().title}", "Chatter")
                        SmartNotify.log("${last.second.id}, ${subList.last().id}", "Chatter")
                        runCatching {
                            list.removeAt(0)
                            notifyItemRemoved(0)
                            val chatter = when (last.first) {
                                Chatter.ALONE -> Chatter.END
                                Chatter.CENTER -> Chatter.CENTER
                                Chatter.END -> Chatter.CENTER
                                Chatter.START -> Chatter.CENTER
                            }
                            list.add(0, Pair(chatter, last.second))
                            notifyItemInserted(0)
                            onScrollRequired?.let { it(list.size) }
                        }
                        addSubList(subList, true, addedLater)
                        return
                    }
                }
            }
            addSubList(subList, false, addedLater)
        }
    }

    private fun addSubList(subList: ArrayList<AppMessage>, link: Boolean, addedLater: Boolean) {
        if (subList.isEmpty()) {
            return
        }
        if (subList.size == 1) {
            val pair =
                if (link) {
                    Pair(Chatter.START, subList.last())
                } else {
                    Pair(Chatter.ALONE, subList.last())
                }
            addItem(pair, addedLater)
            if (addedLater) {
                notifyItemInserted(0)
                onScrollRequired?.let { it(list.size) }
            }
        } else {
            for ((i, subMsg) in subList.withIndex()) {
                val index = when (i) {
                    0 -> {
                        if (link) {
                            Chatter.CENTER
                        } else {
                            Chatter.START
                        }
                    }

                    subList.lastIndex -> {
                        Chatter.END
                    }

                    else -> {
                        Chatter.CENTER
                    }
                }
                addItem(Pair(index, subMsg), addedLater)
            }
            if (addedLater) {
                notifyItemRangeInserted(0, subList.size)
                onScrollRequired?.let { it(list.size) }
            }
        }
    }

    private fun addItem(pair: Pair<Chatter, AppMessage>, addedLater: Boolean) {
        if (addedLater)
            list.add(0, pair)
        else
            list.add(pair)
    }

    override fun readMessage(position: Int) {
        runCatching {
            val item = list[position]
            readMessage(position, item.second)
        }.onFailure { it.printStackTrace() }
    }

    override fun readMessage(msg: AppMessage) {
        runCatching { readMessage(indexOf(msg), msg) }.onFailure { it.printStackTrace() }
    }

    override fun readMessage(position: Int, msg: AppMessage) {
        runCatching {
            if (db.editHelper.readAppMessage(msg.id)) {
                list[position].second.isUnread = false
                notifyItemChanged(position)
                callback?.listDataUpdated(pkg)
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun deleteMessage(position: Int) {
        runCatching {
            deleteMessage(position, list[position].second)
        }.onFailure { it.printStackTrace() }
    }

    override fun deleteMessage(msg: AppMessage) {
        runCatching { deleteMessage(indexOf(msg), msg) }.onFailure { it.printStackTrace() }
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

    private fun indexOf(msg: AppMessage): Int {
        return runCatching {
            list.indexOfFirst { it.second == msg }
        }.getOrElse { -1 }
    }

    private fun indexOf(mid: Int): Int {
        return runCatching {
            list.indexOfFirst { it.second.id == mid }
        }.getOrElse { -1 }
    }


}




