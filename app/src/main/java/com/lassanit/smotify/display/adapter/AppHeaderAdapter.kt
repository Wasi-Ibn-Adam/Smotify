package com.lassanit.smotify.display.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.allViews
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.lassanit.smotify.activities.HomeActivity
import com.lassanit.smotify.bases.CloudBaseHelper
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.bases.SharedBase.Visibility
import com.lassanit.smotify.bases.SmartBase
import com.lassanit.smotify.classes.SortWith
import com.lassanit.smotify.display.adapter.AppHeaderAdapter.ViewType.T_EMPTY
import com.lassanit.smotify.display.adapter.AppHeaderAdapter.ViewType.T_HEADER
import com.lassanit.smotify.display.data.AppHeader
import com.lassanit.smotify.display.holder.AppHeaderHolder
import com.lassanit.smotify.display.holder.EmptyHolder
import com.lassanit.smotify.popups.AppDialog

interface AppHeaderActions {

    fun addHeader(pkg: String): Int?

    fun selectHeader(header: AppHeader, isFirst: Boolean)

    fun readHeaders()
    fun readHeader(position: Int)
    fun readHeader(header: AppHeader)
    fun readHeader(position: Int, header: AppHeader)

    fun visibilityHeader(position: Int)
    fun visibilityHeader(header: AppHeader)
    fun visibilityHeader(position: Int, header: AppHeader)

    fun deleteHeader(position: Int)
    fun deleteHeader(header: AppHeader)
    fun deleteHeader(position: Int, header: AppHeader)

}

class AppHeaderAdapter(
    val activity: FragmentActivity,
    private val typeC: Boolean = Visibility.isCloud()
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), AppHeaderActions {

    companion object {
        private var imp: Callbacks? = null
        fun connect(hImp: Callbacks?) {
            imp = hImp
        }

        fun disconnect() {
            imp = null
        }

        private var selectedMap = setOf<AppHeader>()

        private fun onSelectionToggle(appHeader: AppHeader): Boolean {
            return runCatching {
                if (selectedMap.contains(appHeader)) {
                    selectedMap = selectedMap.minusElement(appHeader)
                    false
                } else {
                    selectedMap = selectedMap.plus(appHeader)
                    true
                }
            }.onFailure { it.printStackTrace() }.getOrElse { false }
        }

        private fun onSelectionStart(appHeader: AppHeader) {
            runCatching {
                selectedMap = setOf(appHeader)
            }.onFailure { it.printStackTrace() }
        }

        private fun isSelected(appHeader: AppHeader): Boolean {
            return runCatching {
                selectedMap.contains(appHeader)
            }.onFailure { it.printStackTrace() }.getOrElse { false }
        }

        private val itemExtension = ItemVisibilityHandler

        fun notificationOpenReset() {
            runCatching { itemExtension.clear() }
        }
    }

    object ItemVisibilityHandler {
        private val lastOpenMap = HashMap<String, Boolean>()

        private fun appOpened(pkg: String) {
            lastOpenMap[pkg] = true
        }

        private fun appClosed(pkg: String) {
            lastOpenMap[pkg] = false
        }

        fun appToggle(pkg: String) {
            if (isAppOpened(pkg))
                appClosed(pkg)
            else
                appOpened(pkg)
        }

        fun isApp(pkg: String): Boolean {
            return lastOpenMap.contains(pkg)
        }

        fun isAppOpened(pkg: String): Boolean {
            return lastOpenMap[pkg] ?: return false
        }

        fun clear() {
            lastOpenMap.clear()
        }
    }

    interface Callbacks {
        fun onItemSelectionActions(
            isActionAllowed: Boolean,
            hasUnread: Boolean,
            isSecretAllowed: Boolean,
        )

        fun onAppHeaderDetailRequired(
            pkg: String,
            isCloud: Boolean
        )
    }

    private object ViewType {
        const val T_EMPTY = -1
        const val T_HEADER = 1
    }

    private var isSelectionOn: Boolean = false
    val db = if (typeC) CloudBaseHelper.get(activity) else SmartBase(activity)
    private val list = ArrayList<Any>()

    init {
        list.clear()
        list.addAll(db.displayHelper.getSingleAppHeaders())
        if (list.size == 0)
            showEmpty()
        datasetUpdated()
        runCatching {
            itemExtension.clear()
        }
    }

    fun isCloudType(): Boolean {
        return typeC
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is AppHeader -> T_HEADER
            is EmptyHolder.Empty -> T_EMPTY
            else -> super.getItemViewType(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            T_HEADER -> AppHeaderHolder.getHolder(activity, parent)
            T_EMPTY -> EmptyHolder.getHolder(activity, parent)
            else -> EmptyHolder.getHolder(activity, parent)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AppHeaderHolder -> {
                val data = list[position] as AppHeader
                holder.handle(activity, data, db, isSelectionOn, isSelected(data), itemExtension)
                holder.onItemClick =
                    { _, b -> imp?.onAppHeaderDetailRequired(b.pkg, isCloudType()) }
                holder.onItemExtend =
                    { _, b -> holder.toggleExtendedView(); itemExtension.appToggle(b.pkg) }
                holder.onItemLongClick = { a, b ->
                    runCatching {
                        val v: View = a.allViews.elementAtOrElse(5) { a.findFocus() }
                        AppDialog.needMenu.make(activity, v, this, b).show()
                    }.onFailure { it.printStackTrace() }
                }
                holder.onItemSelectionClick = { _, b, _ -> selectHeader(b, false) }
                holder.onItemDataChanged = {
                    val newData = db.displayHelper.getSingleAppHeader(data.pkg)
                    newData?.let { it1 ->
                        holder.updateData(it1)
                        list[position] = it1
                    }
                    datasetUpdated()
                }
            }

            is EmptyHolder -> {
                holder.handle(list[position] as EmptyHolder.Empty)
            }
        }
    }

    private fun showEmpty() {
        val type =
            if (isCloudType())
                EmptyHolder.Type.NOTI_CLOUD
            else if (Visibility.Phone.isNormalNotifications())
                EmptyHolder.Type.NOTI_NORMAL
            else
                EmptyHolder.Type.NOTI_SECRET
        list.add(EmptyHolder.Empty(type))
    }

    private fun datasetUpdated() {
        runCatching {
            HomeActivity.getFragImp()
                ?.onItemCountChange(db.displayHelper.getUnreadMessagesCount().toInt())
        }
    }

    fun dataReload() {
        runCatching {
            list.clear()
            list.addAll(db.displayHelper.getSingleAppHeaders())
            if (list.size == 0)
                showEmpty()
            notifyDataSetChanged()
            datasetUpdated()
        }
    }

    override fun selectHeader(header: AppHeader, isFirst: Boolean) {
        runCatching {
            if (isFirst) {
                isSelectionOn = true
                onSelectionStart(header)
                notifyDataSetChanged()
            } else {
                onSelectionToggle(header)
            }
            imp?.onItemSelectionActions(
                selectedMap.isEmpty().not(),
                hasUnreadSelection(),
                isCloudType().not(),
            )
        }
    }

    private fun hasUnreadSelection(): Boolean {
        return runCatching {
            for (item in selectedMap)
                if (item.unread > 0)
                    return true
            return false
        }.onFailure { it.printStackTrace() }.getOrElse { false }
    }

    fun selectionEnd() {
        runCatching {
            isSelectionOn = false
            notifyDataSetChanged()
        }.onFailure { it.printStackTrace() }
    }

    fun deleteSelected() {
        runCatching {
            for (item in selectedMap) {
                deleteHeader(item)
            }
        }.onFailure { it.printStackTrace() }
    }

    fun readSelected() {
        runCatching {
            for (item in selectedMap) {
                readHeader(item)
            }
        }.onFailure { it.printStackTrace() }
    }

    fun toggleVisibilitySelected() {
        runCatching {
            for (item in selectedMap) {
                visibilityHeader(item)
            }
        }.onFailure { it.printStackTrace() }
    }

    fun getItem(position: Int): Any {
        return list[position]
    }

    override fun addHeader(pkg: String): Int? {
        var ret: Int? = null
        runCatching {
            val index = indexOf(pkg)
            ret = if (index == -1) {
                val item = db.displayHelper.getSingleAppHeader(pkg) ?: return@runCatching
                list.add(0, item)
                notifyItemInserted(0)
                datasetUpdated()
                0
            } else {
                val item = db.displayHelper.getSingleAppHeader(pkg) ?: return@runCatching
                list[index] = item
                notifyItemChanged(index)
                if (index != 0) {
                    list.sortWith(SortWith.AppHeaderEmpty)
                    val pos = list.indexOf(item)
                    notifyItemMoved(index, pos)
                }
                datasetUpdated()
                0
            }
        }.onFailure { it.printStackTrace() }
        return if (SharedBase.Settings.Display.isMessageAddedScrollAllowed()) ret else null
    }

    override fun readHeaders() {
        runCatching {
            if (db.editHelper.readAllAppMessages()) {
                dataReload()
                datasetUpdated()
            }
        }.onFailure { it.printStackTrace() }
    }

    private fun indexOf(pkg: String): Int {
        return list.indexOfFirst { it is AppHeader && it.pkg == pkg }
    }

    fun readHeader(pkg: String) {
        runCatching {
            val index = indexOf(pkg)
            if (index != -1) {
                readHeader(index, list[index] as AppHeader)
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun readHeader(position: Int) {
        runCatching {
            val item = list[position]
            if (item is AppHeader)
                readHeader(position, item)
        }.onFailure { it.printStackTrace() }
    }

    override fun readHeader(header: AppHeader) {
        runCatching {
            val pos = list.indexOf(header)
            if (pos != -1)
                readHeader(pos, header)
        }.onFailure { it.printStackTrace() }
    }

    override fun readHeader(position: Int, header: AppHeader) {
        runCatching {
            if (db.editHelper.readAppMessages(header.pkg)) {
                list[position] = db.displayHelper.getSingleAppHeader(header.pkg) ?: return
                notifyItemChanged(position)
                datasetUpdated()
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun visibilityHeader(position: Int) {
        runCatching {
            val item = list[position]
            if (item is AppHeader)
                visibilityHeader(position, item)
        }.onFailure { it.printStackTrace() }
    }

    override fun visibilityHeader(header: AppHeader) {
        runCatching {
            val pos = list.indexOf(header)
            if (pos != -1)
                visibilityHeader(pos, header)
        }.onFailure { it.printStackTrace() }
    }

    override fun visibilityHeader(position: Int, header: AppHeader) {
        runCatching {
            if (isCloudType())
                return@runCatching
            if (db.displayHelper.toggleAppHeaderVisibility(header.pkg)) {
                list.removeAt(position)
                notifyItemRemoved(position)
                if (list.isEmpty()) {
                    showEmpty()
                    notifyItemInserted(0)
                }
                datasetUpdated()
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun deleteHeader(position: Int) {
        runCatching {
            val item = list[position]
            if (item is AppHeader)
                deleteHeader(position, item)
        }.onFailure { it.printStackTrace() }
    }

    override fun deleteHeader(header: AppHeader) {
        runCatching {
            val pos = list.indexOf(header)
            if (pos != -1)
                deleteHeader(pos, header)
        }.onFailure { it.printStackTrace() }
    }

    override fun deleteHeader(position: Int, header: AppHeader) {
        runCatching {
            if (position >= 0) {
                if (db.editHelper.deleteAppMessages(header.pkg)) {
                    list.removeAt(position)
                    notifyItemRemoved(position)
                    if (list.isEmpty()) {
                        showEmpty()
                        notifyItemInserted(0)
                    }
                    datasetUpdated()
                }
            }
        }.onFailure { it.printStackTrace() }
    }

}