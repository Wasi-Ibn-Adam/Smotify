package com.lassanit.smotify.display.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails
import com.lassanit.smotify.display.holder.EmptyHolder
import com.lassanit.smotify.display.holder.SubscriptionHolder

interface SubscriptionImp {
    fun onSelect(sub: ProductDetails.SubscriptionOfferDetails)
    fun onManagerRestartRequired()
}

class SubscriptionAdapter(
    private val list: List<Any>,
    private val imp: SubscriptionImp,
    private val details: ArrayList<String>
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private object ViewType {
        const val SUB = 0
        const val EMPTY = 1
    }


    private object Colors{
        val list= listOf("#BFBE61F8","#BFEB62B0","#BFFEB626","#BF5D76DB")
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SUB -> {
                SubscriptionHolder.getHolder(parent)
            }

            ViewType.EMPTY -> {
                EmptyHolder.getHolder(parent.context, parent)
            }

            else -> {
                SubscriptionHolder.getHolder(parent)
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is ProductDetails.SubscriptionOfferDetails -> {
                ViewType.SUB
            }

            is EmptyHolder.Empty -> {
                ViewType.EMPTY
            }

            else -> {
                super.getItemViewType(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        runCatching {
            if (holder is SubscriptionHolder) {
                val sub = list[position] as ProductDetails.SubscriptionOfferDetails
                val detail = details.getOrNull(position)
                holder.handle(sub, detail)
                runCatching {
                    val value= Colors.list[position.mod(Colors.list.size)]
                    val color=Color.parseColor(value)
                    holder.itemView.backgroundTintList= ColorStateList.valueOf(color)

                }

                holder.itemView.setOnClickListener { imp.onSelect(sub) }
            } else if (holder is EmptyHolder) {
                holder.handle(list[position] as EmptyHolder.Empty)
                imp.onManagerRestartRequired()
            }
        }
    }


}