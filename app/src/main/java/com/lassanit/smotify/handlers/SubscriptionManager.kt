package com.lassanit.smotify.handlers

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.CloudBase
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.classes.SortWith
import java.util.Date

interface SubscriptionManagerImp {
    fun onLog(string: String)
    fun onToast(string: String)
    fun onWaiting(state: Boolean)
    fun refresh()
    fun purchaseComplete()
}

class SubscriptionManager(context: Context) : PurchasesUpdatedListener, BillingClientStateListener,
    ProductDetailsResponseListener, AcknowledgePurchaseResponseListener, PurchasesResponseListener {
    enum class Type { ADS, CLOUD }
    private object IDS {
        const val ADS = "1_ad"
        const val CLOUD = "1_cloud"
    }

    object TypeDetails {
        // rule :: time-name   --> time in days

        private fun toTitleCase(string: String): String {
            return string.first().titlecase().plus(string.substring(1))
        }

        private fun getTime(time: Long): Pair<Long, String> {
            val count = time / (24 * 60 * 60 * 1000)
            return if (count < 7) {
                Pair(count, "Day")
            } else if (count < 30) {
                Pair(count / 7, "Week")
            } else if (count < 360) {
                Pair(count / 30, "Month")
            } else {
                Pair(count / 360, "Year")
            }
        }

        /* first is price second is time */
        private fun getDiscount(base: Pair<Long, Long>, offer: Pair<Long, Long>): Int {
            return runCatching {
                val fold = offer.second / base.second
                val originalPrice = fold * base.first
                (((originalPrice - offer.first).toDouble() / originalPrice) * 100.0).toInt()
            }.onFailure { it.printStackTrace() }.getOrElse { 0 }
        }

        fun getSubscriptionOfferName(sub: SubscriptionOfferDetails): String {
            val tokens = sub.basePlanId.lowercase().split("-")
            return if (tokens.size > 1) buildString {
                for (i in 1 until tokens.size)
                    append(toTitleCase(tokens[i])).append(' ')
                append(' ')
            }.trim()
            else
                "Plan"
        }

        fun getDetails(type: Type, subs: List<SubscriptionOfferDetails>?): ArrayList<String> {
            if (subs.isNullOrEmpty())
                return arrayListOf()
            return runCatching {
                val list = arrayListOf<Triple<Long, String, Long>>()
                for (sub in subs) {
                    val time = getSubscriptionDuration(sub)
                    val price = sub.pricingPhases.pricingPhaseList.first().formattedPrice
                    val milPrice = sub.pricingPhases.pricingPhaseList.first().priceAmountMicros
                    list.add(Triple(milPrice, price, time))
                }

                val resList = ArrayList<String>()
                for (item in list) {
                    resList.add(
                        buildString {
                            val timePair = getTime(item.third)
                            append(timePair.first).append(' ').append(timePair.second)
                            append(
                                when (type) {
                                    Type.ADS -> " No Ads"; Type.CLOUD -> " Access"
                                }
                            ).appendLine().appendLine()

                            //     append("Only ").append(item.second).appendLine().appendLine()

                            val dis = getDiscount(
                                Pair(list[0].first, list[0].third),
                                Pair(item.first, item.third)
                            )
                            if (dis > 0) append("Save ").append(dis).append('%')

                        }
                    )
                }
                return resList
            }.onFailure { it.printStackTrace() }.getOrElse { arrayListOf() }
        }

        fun getSubscriptionDuration(planId: String): Long {
            val tokens = planId.lowercase().split("-")
            return runCatching {
                if (tokens.size > 1) {
                    for (token in tokens) {
                        return token.toLongOrNull() ?: continue
                    }
                    0L
                } else {
                    0L
                }
            }.getOrElse { 0L }
        }

        fun getSubscriptionDuration(sub: SubscriptionOfferDetails): Long {
            return getSubscriptionDuration(sub.basePlanId)
        }

        fun getProductId(type: Type): String {
            return when (type) {
                Type.ADS -> IDS.ADS
                Type.CLOUD -> IDS.CLOUD
            }
        }

        fun getTypeByIds(id: String): Type {
            return when (id) {
                IDS.ADS -> Type.ADS
                IDS.CLOUD -> Type.CLOUD
                else -> Type.ADS
            }
        }
    }

    companion object {
        private const val TAG = "SubscriptionManager"
        private var imp: SubscriptionManagerImp? = null

        fun link(imp: SubscriptionManagerImp?) {
            this.imp = imp
        }

        fun unlink() {
            runCatching { imp = null }
        }

        fun removeDuplicateSubscription(subs: List<SubscriptionOfferDetails>?): List<SubscriptionOfferDetails> {
            return runCatching {
                if (subs.isNullOrEmpty())
                    return arrayListOf()
                val list = arrayListOf<SubscriptionOfferDetails>()

                for (sub in subs.sortedWith(SortWith.SubSort)) {
                    if (list.any { it.basePlanId == sub.basePlanId }.not()) {
                        list.add(sub)
                    } else {
                        if (sub.offerId != null)
                            if (list.removeIf { it.basePlanId == sub.basePlanId && it.offerId == null && sub.offerId != null }) {
                                list.add(sub)
                            }
                    }
                }
                list.sortWith(SortWith.SubSort)
                list
            }.getOrElse { arrayListOf() }
        }
    }

    private var type: Type? = null
    private val billingClient =
        BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
    private val productDetailList = ArrayList<ProductDetails>()

    fun subscribeOffer(
        activity: AppCompatActivity,
        productDetails: ProductDetails,
        sub: SubscriptionOfferDetails
    ) {
        runCatching {
            type = TypeDetails.getTypeByIds(productDetails.productId)
            when (type) {
                Type.ADS -> {
                    SharedBase.Ads.setTempId(sub.basePlanId)
                }

                Type.CLOUD -> {
                    SharedBase.CloudService.Purchase.setTempId(sub.basePlanId)
                }

                else -> {}
            }
            imp?.onWaiting(true)
            val productDetailsParams: BillingFlowParams.ProductDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(sub.offerToken)
                    .build()
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()
            billingClient.launchBillingFlow(activity, params)
        }.onFailure { it.printStackTrace() }
    }

    init {
        billingClient.startConnection(this)
    }

    fun getProductDetails(type: Type): ProductDetails? {
        runCatching {
            val id = TypeDetails.getProductId(type)
            for (product in productDetailList)
                if (product.productId == id)
                    return product
        }
        return null
    }

    private fun initiatePurchases() {
        runCatching {
            if (billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
                    .responseCode == BillingResponseCode.OK
            ) {
                val list = arrayListOf<QueryProductDetailsParams.Product>()
                for (type in Type.values()) {
                    val id = TypeDetails.getProductId(type)
                    onLog("Product ID: $id")
                    list.add(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS)
                            .setProductId(id)
                            .build()
                    )
                }
                val query = QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(list)
                    .build()
                billingClient.queryProductDetailsAsync(query, this)
            } else {
                onLog("Sorry Subscription not Supported. Please Update Play Store")
                imp?.onToast("Sorry Subscription not Supported. Please Update Play Store")
            }
        }.onFailure { it.printStackTrace() }
    }

    private var purchase: Purchase? = null

    private fun productPurchasedNow(purchase: Purchase) {
        runCatching {
            //onLog(
            //    buildString {
            //        append("Purchased New: ").appendLine()
            //        append("Purchased products: ").append(purchase.products).appendLine()
            //        append("   Order Id: ").append(purchase.orderId).appendLine()
            //        append("   Order Acknowledged: ").append(purchase.isAcknowledged).appendLine()
            //        append("   Order quantity: ").append(purchase.quantity).appendLine()
            //        append("   Order AutoRenewing: ").append(purchase.isAutoRenewing).appendLine()
            //        append("   Purchasing Date: ").append(Date(purchase.purchaseTime))
            //            .appendLine()
            //    }
            //)
            this.purchase = purchase
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams, this)
        }.onFailure { it.printStackTrace();imp?.onWaiting(false) }
    }

    private fun productPurchased(purchase: Purchase) {
        runCatching {
            onLog(
                buildString {
                    append("Purchased OLD: ").appendLine()
                    append("Purchased products: ").append(purchase.products).appendLine()
                    append("   Order Id: ").append(purchase.orderId).appendLine()
                    append("   Order Acknowledged: ").append(purchase.isAcknowledged).appendLine()
                    append("   Order quantity: ").append(purchase.quantity).appendLine()
                    append("   Order AutoRenewing: ").append(purchase.isAutoRenewing).appendLine()
                    append("   Purchasing Date: ").append(Date(purchase.purchaseTime)).appendLine()
                    append("   Original Json: ").append(purchase.originalJson).appendLine()
                }
            )
            handleAcknowledgement(purchase)

        }.onFailure { it.printStackTrace();imp?.onWaiting(false) }
    }

    private fun productFound(productDetails: ProductDetails) {
        onLog(
            "productFound::Name:${productDetails.name}," +
                    "PID:${productDetails.productId} \n" +
                    "\tType:${productDetails.productType} \n" +
                    "\tTitle:${productDetails.title} \n" +
                    "\tDesc:${productDetails.description} \n" +
                    "\tSubs:\n${
                        buildString {
                            append('\t').append('\t').append("Size:")
                                .append(productDetails.subscriptionOfferDetails?.size).append('\n')
                            if (productDetails.subscriptionOfferDetails != null) {
                                for (a in productDetails.subscriptionOfferDetails!!)
                                    append('\t').append('\t').append(a.basePlanId).append('\n')
                                        .append('\t').append('\t').append('\t').append(a.offerId)
                                        .append('\n')
                                        .append('\t').append('\t').append('\t')
                                        .append(a.offerTags.toString()).append('\n')
                                        .append('\t').append('\t').append('\t').append(a.offerToken)
                                        .append('\n')
                                        .append('\t').append('\t').append('\t')
                                        .append(a.pricingPhases.pricingPhaseList.first().formattedPrice)
                                        .append('\n')
                            }
                        }
                    } \n" +
                    "\tOTP Price :${productDetails.oneTimePurchaseOfferDetails?.formattedPrice} \n" +
                    "\tDesc:${productDetails.description},"
        )

        runCatching {
            for (product in productDetailList) {
                if (product.productId == productDetails.productId)
                    return
            }
            productDetailList.add(productDetails)
        }.onFailure { it.printStackTrace() }
    }

    private fun checkPurchases(purchases: List<Purchase>?, old: Boolean = false) {
        runCatching {
            if (purchases.isNullOrEmpty()) {
                onLog("checkPurchases:: No Purchase Found")
                imp?.onWaiting(false)
                return
            }
            onLog("checkPurchases::Size:${purchases.size}")
            for (purchase in purchases) {
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> {
                        onLog("Subscription is Purchased. ${purchase.products}")
                        if (purchase.isAcknowledged) {
                            productPurchased(purchase)
                        } else {
                            productPurchasedNow(purchase)
                        }
                    }

                    Purchase.PurchaseState.PENDING -> {
                        onLog("Subscription is Pending. Please Wait!")
                        imp?.onToast("Subscription is Pending. Please Wait!")
                    }

                    Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                        onLog("Subscription state is Unknown.")
                        imp?.onToast("Subscription state is Unknown.")
                    }
                }
            }
        }.onFailure { it.printStackTrace(); imp?.onWaiting(false) }
    }

    private fun checkProducts(products: List<ProductDetails>?) {
        runCatching {
            if (products.isNullOrEmpty()) {
                onLog("checkProducts:: No Product Found")
                return
            }
            onLog("checkProducts::${products.size}")
            for (product in products) {
                if (product.subscriptionOfferDetails.isNullOrEmpty()) {
                    onLog("checkProducts::${product.productId} :has no subscriptions")
                    imp?.onToast("No Plan exist Yet.")
                    return
                }
                productFound(product)
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
        runCatching {
            when (p0.responseCode) {
                BillingResponseCode.OK -> {
                    onLog("onPurchasesUpdated::OK")
                    checkPurchases(p1)
                }

                BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    onLog("onPurchasesUpdated::ITEM_ALREADY_OWNED")
                    billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS).build(),
                        this
                    )
                    checkPurchases(p1)
                }

                BillingResponseCode.USER_CANCELED -> {
                    onLog("onPurchasesUpdated::USER_CANCELED")
                    imp?.onWaiting(false)
                }

                BillingResponseCode.BILLING_UNAVAILABLE -> {
                    onLog("onPurchasesUpdated::BILLING_UNAVAILABLE:: ${p0.debugMessage}")
                    imp?.onToast(
                        buildString {
                            append("Payment not Successful, Possible Reasons can be: ").appendLine()
                            append("   1 -> The Play Store app is not updated.").appendLine()
                            append("   2 -> Unsupported country.").appendLine()
                            append("   3 -> Google Play is unable to charge the with current Payment method.").appendLine()
                            append("Try again, If error remains then check above Issues").appendLine()
                        }
                    )
                    imp?.onWaiting(false)
                }

                BillingResponseCode.NETWORK_ERROR -> {
                    onLog("onPurchasesUpdated::NETWORK_ERROR:: ${p0.debugMessage}")
                    imp?.onToast("Please check your Network connection")
                    imp?.onWaiting(false)
                }

                else -> {
                    onLog("onPurchasesUpdated::else::${p0.responseCode}, ${p0.debugMessage}")
                    imp?.onWaiting(false)
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun onBillingServiceDisconnected() {
        onLog("onBillingServiceDisconnected")
    }

    override fun onBillingSetupFinished(p0: BillingResult) {
        runCatching {
            when (p0.responseCode) {
                BillingResponseCode.OK -> {
                    onLog("onBillingSetupFinished")
                    billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS).build(),
                        this
                    )
                }

                else -> {
                    onLog("onBillingSetupFinished::else::${p0.responseCode}, ${p0.debugMessage}")
                }
            }
            initiatePurchases()
        }.onFailure { it.printStackTrace() }
    }

    override fun onProductDetailsResponse(p0: BillingResult, p1: MutableList<ProductDetails>) {
        runCatching {
            when (p0.responseCode) {
                BillingResponseCode.OK -> {
                    onLog("onProductDetailsResponse")
                    checkProducts(p1)
                }

                else -> {
                    onLog("onProductDetailsResponse::else::${p0.responseCode}, ${p0.debugMessage}")
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    private fun handleAcknowledgement(purchase: Purchase?) {
        onLog("handleAcknowledgement $purchase")
        if (purchase == null || purchase.products.isEmpty()) {
            imp?.onWaiting(false)
            return
        }

        for ((i, product) in purchase.products.withIndex()) {
            onLog("handleAcknowledgement product:: $product")
            when (val t = type ?: TypeDetails.getTypeByIds(product)) {
                Type.ADS -> {
                    onLog("handleAcknowledgement type:: $t")
                    val id = SharedBase.Ads.getTempId()
                    if (id == null) {
                        if (i == purchase.products.lastIndex) {
                            imp?.onWaiting(false)
                            return
                        }
                        continue
                    }
                    val time =
                        TypeDetails.getSubscriptionDuration(id).plus(System.currentTimeMillis())
                    CloudBase.RealTime.setAdTimeLimit(time, onSuccess = {
                        SharedBase.Ads.setTime(time)
                        SharedBase.Ads.removeTempId()
                    }, onComplete = {
                        imp?.refresh()
                        finish()
                    })
                }

                Type.CLOUD -> {
                    onLog("handleAcknowledgement type:: $t")
                    val id = SharedBase.CloudService.Purchase.getTempId()
                    if (id == null) {
                        if (i == purchase.products.lastIndex) {
                            imp?.onWaiting(false)
                            return
                        }
                        continue
                    }
                    val time = TypeDetails.getSubscriptionDuration(id).plus(Date().time)
                    CloudBase.RealTime.setCloudTimeLimit(time, onSuccess = {
                        SharedBase.CloudService.setTime(time)
                        SharedBase.CloudService.Purchase.removeTempId()
                    }, onComplete = {
                        imp?.refresh()
                        finish()
                    })
                }
            }
        }
    }

    override fun onAcknowledgePurchaseResponse(p0: BillingResult) {
        runCatching {
            when (p0.responseCode) {
                BillingResponseCode.OK -> {
                    onLog("onAcknowledgePurchaseResponse")
                    handleAcknowledgement(purchase)
                }

                else -> {
                    onLog("onAcknowledgePurchaseResponse::else::${p0.responseCode}, ${p0.debugMessage}")
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun onQueryPurchasesResponse(p0: BillingResult, p1: MutableList<Purchase>) {
        runCatching {
            when (p0.responseCode) {
                BillingResponseCode.OK -> {
                    onLog("onQueryPurchasesResponse")
                    checkPurchases(p1, true)
                }

                else -> {
                    onLog("onQueryPurchasesResponse::else::${p0.responseCode}, ${p0.debugMessage}")
                }
            }
            imp?.onWaiting(false)
        }.onFailure { it.printStackTrace() }
    }

    private fun onLog(string: String) {
        SmartNotify.log(string, TAG)
        imp?.onLog(string)
    }

    private fun finish() {
        runCatching {
            onLog("Subscription is Completed.")
            imp?.onToast("Restart the App, if changes are not made.")
            imp?.onWaiting(false)
            imp?.purchaseComplete()
            //billingClient.endConnection()
        }.onFailure { it.printStackTrace() }
    }

}