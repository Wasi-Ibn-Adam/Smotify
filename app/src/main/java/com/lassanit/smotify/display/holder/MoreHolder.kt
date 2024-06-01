package com.lassanit.smotify.display.holder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lassanit.smotify.R

class MoreHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    constructor(context: Context, parent: ViewGroup?) : this(getView(context, parent))

    companion object {
        fun getHolder(context: Context, parent: ViewGroup): MoreHolder {
            return MoreHolder(
                DataHolder.inflate(
                    context,
                    parent,
                    R.layout.holder_row_more
                )
            )
        }

        private fun getView(context: Context, parent: ViewGroup?): View {
            return LayoutInflater.from(context).inflate(
                R.layout.holder_row_more,
                parent,
                false
            )
        }

        fun isMoreView(view: View): Boolean {
            runCatching { return (view.findViewById<View>(R.id.holder_row_more_text) != null) }
            return false
        }
    }

    private val textT: TextView = view.findViewById(R.id.holder_row_more_text)
    fun handle(runnable: Runnable): MoreHolder {
        textT.setOnClickListener { runnable.run() }
        this.view.setOnClickListener { runnable.run() }
        return this
    }

    fun getView(): View {
        return view
    }

    class More(val runnable: Runnable) {}
}