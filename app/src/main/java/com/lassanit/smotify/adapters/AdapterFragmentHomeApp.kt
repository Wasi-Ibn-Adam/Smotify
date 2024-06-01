package com.lassanit.smotify.adapters

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lassanit.smotify.bases.SmartBase
import com.lassanit.smotify.classes.SortWith
import com.lassanit.smotify.display.holder.AppHolder

class AdapterFragmentHomeApp(
    context: Context,
) :
    RecyclerView.Adapter<AppHolder>() {
    private val db = SmartBase(context).displayHelper
    private var list = db.getAppList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppHolder {
        return AppHolder.getHolder(parent)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(hold: AppHolder, position: Int) {
        val app = list[position]
        hold.handle(position, app)
    }

    fun appAdded(pkg: String) {
        runCatching {
            if (list.any { it.pkg == pkg }) {
                return
            }
            val app = db.getApp(pkg) ?: return
            list.add(app)
            list.sortWith(SortWith.SmartApp)
            val index = list.indexOfFirst { it.pkg == pkg }
            if (index > -1) {
                notifyItemInserted(index)
            }
        }
    }

    fun appRemoved(pkg: String) {
        runCatching {
            if (list.any { it.pkg == pkg }.not()) {
                return
            }
            val index = list.indexOfFirst { it.pkg == pkg }
            if (index > -1) {
                list.removeAt(index)
                notifyItemRemoved(index)
            }
        }
    }

    fun appUpdated(pkg: String) {
        reload()
    }

    fun reload() {
        runCatching {
            list = db.getAppList()
            notifyDataSetChanged()
        }.onFailure { it.printStackTrace() }
    }

}