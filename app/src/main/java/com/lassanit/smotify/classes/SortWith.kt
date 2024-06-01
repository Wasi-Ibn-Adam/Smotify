package com.lassanit.smotify.classes

import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.display.data.AppHeader
import com.lassanit.smotify.display.data.AppMessage


class SortWith {

    companion object Comparator {

        val SubSort = java.util.Comparator<SubscriptionOfferDetails> { t1, t2 ->
            t1.pricingPhases.pricingPhaseList.first()
                .priceAmountMicros.compareTo(t2.pricingPhases.pricingPhaseList.first().priceAmountMicros)
        }

        val SmartApp = java.util.Comparator<App> { app, t1 ->
            if (t1.name.isEmpty())
                app.pkg.compareTo(t1.pkg)
            else
                app.name.lowercase().compareTo(t1.name.lowercase())
        }

        val AppHeader = java.util.Comparator<AppHeader> { h, t1 ->
            t1.time.compareTo(h.time)
        }
        val AppHeaderEmpty= java.util.Comparator<Any> { h, t1 ->
            if(h is AppHeader && t1 is AppHeader){
                t1.time.compareTo(h.time)
            }
            else 0
        }
        val AppMessage = java.util.Comparator<AppMessage> { m1, m2 ->
            if (m1.time != m2.time)
                m2.time.compareTo(m1.time)
            else
                m2.id.compareTo(m1.id)
        }

    }

    object Chooser {


        fun <T : Comparable<T>> greater(a: T, b: T): T {
            return if (a > b) a else b
        }

        fun <T : Comparable<T>> smaller(a: T, b: T): T {
            return if (a < b) a else b
        }
    }
}