package com.lassanit.smotify.display.holder

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.lassanit.smotify.R
import com.lassanit.smotify.handlers.SubscriptionManager

class SubscriptionHolder(private val v: View) : RecyclerView.ViewHolder(v) {

    companion object {
        fun getHolder(parent: ViewGroup): SubscriptionHolder {
            return SubscriptionHolder(
                DataHolder.inflate(
                    parent.context,
                    parent,
                    R.layout.holder_row_subscription
                )
            )
        }
    }

    private val titleT: TextView = v.findViewById(R.id.product_title)
    private val descT: TextView = v.findViewById(R.id.product_desc)
    private val priceT: TextView = v.findViewById(R.id.product_price)

    fun handle(sub: SubscriptionOfferDetails, details: String?) {
        titleT.text = SubscriptionManager.TypeDetails.getSubscriptionOfferName(sub)
        descT.text = details
        priceT.text = sub.pricingPhases.pricingPhaseList.first().formattedPrice
    }

}