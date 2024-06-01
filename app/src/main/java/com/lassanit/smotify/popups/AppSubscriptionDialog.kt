package com.lassanit.smotify.popups

import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.lassanit.extras.WaitingDialog
import com.lassanit.smotify.R
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.CloudBase
import com.lassanit.smotify.display.adapter.SubscriptionAdapter
import com.lassanit.smotify.display.adapter.SubscriptionImp
import com.lassanit.smotify.display.holder.EmptyHolder
import com.lassanit.smotify.handlers.SubscriptionManager
import com.lassanit.smotify.handlers.SubscriptionManagerImp

class AppSubscriptionDialog(
    private val activity: AppCompatActivity,
    private val type: SubscriptionManager.Type,
    private val onSubscribe: () -> Unit
) : AppDialog(activity, false), SubscriptionManagerImp, SubscriptionImp {
    companion object {
        private const val TAG = "AppSubscriptionDialog"
        fun makeAds(activity: AppCompatActivity, onSubscribe: () -> Unit): AppSubscriptionDialog {
            return AppSubscriptionDialog(activity, SubscriptionManager.Type.ADS, onSubscribe)
        }

        fun makeCloud(activity: AppCompatActivity, onSubscribe: () -> Unit): AppSubscriptionDialog {
            return AppSubscriptionDialog(activity, SubscriptionManager.Type.CLOUD, onSubscribe)
        }
    }

    private val recyclerView: RecyclerView
    private val btnCancel: Button
    private val titleT: TextView
    private val descT: TextView

    private val pop = WaitingDialog(activity)
    private val product: ProductDetails? =
        SmartNotify.getSubscriptionManager().getProductDetails(type)
    private var details = ArrayList<String>()

    init {
        setContentView(R.layout.dialog_app_subscription)
        recyclerView = findViewById(R.id.dialog_app_subscription_recycler)
        btnCancel = findViewById(R.id.dialog_app_subscription_cancel)
        titleT = findViewById(R.id.dialog_app_subscription_title)
        descT = findViewById(R.id.dialog_app_subscription_desc)

        runCatching {
            val list =
                SubscriptionManager.removeDuplicateSubscription(product?.subscriptionOfferDetails)
            runCatching {
                details = SubscriptionManager.TypeDetails.getDetails(type, list)
            }.onFailure { it.printStackTrace() }


            recyclerView.layoutManager =
                if (list.isEmpty()) LinearLayoutManager(context)
                else GridLayoutManager(context, 2)
            recyclerView.setHasFixedSize(false)

            recyclerView.adapter =
                SubscriptionAdapter(
                    list.ifEmpty { listOf(EmptyHolder.Empty(EmptyHolder.Type.SUBS)) },
                    this, details
                )
        }.onFailure { it.printStackTrace() }
        runCatching {
            this.titleT.text = product?.name
            this.descT.text = product?.description
            btnCancel.setOnClickListener { dismiss() }
        }.onFailure { it.printStackTrace() }

        runCatching {
            SubscriptionManager.link(this)
            setOnDismissListener {
                SubscriptionManager.unlink()
            }

            initFinalize(fullView = true)
        }.onFailure { it.printStackTrace() }

    }

    override fun onSelect(sub: ProductDetails.SubscriptionOfferDetails) {
        runCatching {
            product?.let { SmartNotify.getSubscriptionManager().subscribeOffer(activity, it, sub) }
        }.onFailure { it.printStackTrace() }
    }

    override fun onManagerRestartRequired() {
        runCatching {
            SmartNotify.setupBillingCheck(activity)
            SubscriptionManager.link(this)
        }.onFailure { it.printStackTrace() }
    }

    override fun onLog(string: String) {
        runCatching {
            SmartNotify.log(string, TAG)
        }.onFailure { it.printStackTrace() }
    }

    override fun onToast(string: String) {
        runCatching {
            activity.runOnUiThread {
                Toast.makeText(activity, string, Toast.LENGTH_SHORT).show()
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun onWaiting(state: Boolean) {
        runCatching {
            activity.runOnUiThread {
                if (state)
                    pop.show()
                else pop.dismiss()
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun refresh() {
        // runCatching { this.dialog.dismiss(); activity.onBackPressedDispatcher.onBackPressed() }
    }

    override fun purchaseComplete() {
        runCatching {
            when (type) {
                SubscriptionManager.Type.ADS -> {}
                SubscriptionManager.Type.CLOUD -> {
                    Firebase.auth.currentUser?.uid?.let {
                        CloudBase.Store.set(
                            it,
                            true
                        )
                    }
                }
            }
        }

        activity.runCatching {
            activity.runOnUiThread {
                onSubscribe();
            }
            dismiss()
        }.onFailure { it.printStackTrace();dismiss() }
    }
}