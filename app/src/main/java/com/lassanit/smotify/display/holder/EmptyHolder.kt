package com.lassanit.smotify.display.holder

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lassanit.smotify.R

class EmptyHolder(val view: View) : RecyclerView.ViewHolder(view) {
    companion object {
        fun getHolder(context: Context, parent: ViewGroup): EmptyHolder {
            return EmptyHolder(
                DataHolder.inflate(
                    context,
                    parent,
                    R.layout.holder_row_empty
                )
            )
        }
    }

    private val loadingI: ImageView = view.findViewById(R.id.holder_row_empty_img)
    private val typeT: TextView = view.findViewById(R.id.holder_row_empty_text)
    fun handle(empty: Empty) {
        typeT.text = when (empty.type) {
            Type.NOTI_NORMAL -> {
                Glide.with(view.context)
                    .asGif()
                    .load(com.lassanit.firekit.R.drawable.loader2)
                    .into(loadingI)
                "Notifications on their way!"
            }

            Type.NOTI_SECRET -> {
                Glide.with(view.context)
                    .asDrawable()
                    .load(R.drawable.base_secret)
                    .into(loadingI)
                "No App Notifications are currently secret."
            }

            Type.SUBS -> {
                Glide.with(view.context)
                    .asDrawable()
                    .load(R.drawable.base_ads)
                    .into(loadingI)
                "We're not able to find any subscription plans for you right now. Please check your network connection and try again."
            }

            Type.NOTI_CLOUD -> {
                Glide.with(view.context)
                    .asGif()
                    .load(com.lassanit.firekit.R.drawable.loader2)
                    .into(loadingI)
                "We're still waiting for your cloud sharing notification to arrive. Please note that cloud sharing notifications can take up to 15-20 minutes to arrive, as they are not real-time. In the meantime, please check your network connection and make sure that cloud sharing is not blocked in the app settings of either device."
            }
        }
    }

    class Empty(val type: Type) {}
    enum class Type {
        NOTI_NORMAL, NOTI_SECRET, NOTI_CLOUD, SUBS
    }
}