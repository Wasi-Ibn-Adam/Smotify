package com.lassanit.smotify.display.holder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView

open class DataHolder(var view: View) : RecyclerView.ViewHolder(view) {
    companion object {
        fun inflate(context: Context, parent: ViewGroup, res: Int): View {
            return LayoutInflater.from(context).inflate(res, parent, false)
        }
    }

    class HolderListener(
        var onClick: View.OnClickListener? = null,
        var onLongClick: View.OnLongClickListener? = null,
        var onCheck: CompoundButton.OnCheckedChangeListener? = null,
        var onMediaClick: View.OnClickListener? = null,
    ) {}



}