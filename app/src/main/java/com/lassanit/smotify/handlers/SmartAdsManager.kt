package com.lassanit.smotify.handlers

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.lassanit.smotify.R
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.display.adapter.AdmobNativeAdAdapter
import java.util.Date

class SmartAdsManager {

    companion object {
        const val LOG_TAG: String = "SmartAdsManager"
        private lateinit var openAd: AppOpenAds
        private lateinit var bannerAds: BannerAds
        private lateinit var interstitialAd: InterstitialAds
        private lateinit var rewardAd: RewardAds
        private var adCount = 0L


        fun initialize(context: Context) {
            MobileAds.initialize(context) { }
            openAd = AppOpenAds()
            //openAd.requestNewAd(context.applicationContext) // must be called in Main UI thread but 'fun initialize(context: Context) ' is being called in IO thread
            bannerAds = BannerAds()
            interstitialAd = InterstitialAds()
            rewardAd = RewardAds()
        }

        fun linkBannerAds(activity: Activity) {
            runCatching {
                if (::bannerAds.isInitialized) {
                    bannerAds.linkAdView(activity, R.id.adView)
                }
            }.onFailure { it.printStackTrace() }
        }

        fun requestBannerAds() {
            runCatching {
                if (::bannerAds.isInitialized) {
                    bannerAds.requestNewAd()
                }
            }.onFailure { it.printStackTrace() }
        }

        fun unlinkBannerAds() {
            runCatching {
                if (::bannerAds.isInitialized) {
                    bannerAds.unlinkAdView()
                }
            }.onFailure { it.printStackTrace() }
        }

        fun requestAppOpenAd(activity: Activity) {
            runCatching {
                if (::openAd.isInitialized)
                    openAd.display(activity)
            }
        }

        fun requestInterstitialAd(activity: Activity) {
            runCatching {
                if (::openAd.isInitialized.not()
                        .or(!openAd.isAdAvailable() || !openAd.isAdShowing())
                ) {
                    adCount++
                    if (adCount.mod(3) == 0)
                        if (::interstitialAd.isInitialized)
                            interstitialAd.display(activity)
                }
            }
        }

        fun requestRewardLoadOnly(activity: Activity) {
            runCatching {
                if (::rewardAd.isInitialized)
                    rewardAd.requestNewAd(activity)
            }
        }

        fun hasRewardAd(): Boolean {
            return runCatching { if (::rewardAd.isInitialized) rewardAd.hasAd() else false }.getOrElse { false }
        }

        fun requestRewardDisplay(activity: Activity, rewardListener: () -> Unit) {
            runCatching {
                if (::rewardAd.isInitialized)
                    rewardAd.display(activity, rewardListener)
            }
        }


        fun convertAdAdapter(
            context: Context,
            adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
            interval: Int = 7,
            type: String = AdmobNativeAdAdapter.TYPE_SMALL
        ): RecyclerView.Adapter<RecyclerView.ViewHolder> {
            return if (SharedBase.Ads.isActive().or(SharedBase.Ads.isTrailActive())
                    .or(SmartNotify.isLoggedIn().not())
            )
                adapter
            else AdmobNativeAdAdapter.Builder.with(
                context.resources.getString(R.string.ad_id_native),
                adapter,
                type
            ).adItemInterval(interval).build()
        }
    }

    class BannerAds {
        private var ads: AdView? = null
        private val listener: AdListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                SmartNotify.log("BannerAds::onAdLoaded", LOG_TAG)
                if (SharedBase.Ads.isActive().or(SmartNotify.isLoggedIn().not()))
                    return
                if (SharedBase.Ads.isTrailActive())
                    return
                ads?.visibility = View.VISIBLE
            }

            override fun onAdClicked() {
                super.onAdClicked()
                SmartNotify.log("BannerAds::onAdClicked", LOG_TAG)
            }

            override fun onAdClosed() {
                super.onAdClosed()
                SmartNotify.log("BannerAds::onAdClosed", LOG_TAG)
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                SmartNotify.log("BannerAds::onAdFailedToLoad: ${p0.message}", LOG_TAG)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                SmartNotify.log("BannerAds::onAdImpression", LOG_TAG)
            }

            override fun onAdOpened() {
                super.onAdOpened()
                SmartNotify.log("BannerAds::onAdOpened", LOG_TAG)
            }

            override fun onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked()
                SmartNotify.log("BannerAds::onAdSwipeGestureClicked", LOG_TAG)
            }
        }

        init {
            ads?.visibility = View.GONE
            ads?.adListener = listener
        }

        fun requestNewAd() {
            SmartNotify.log("BannerAds::onNewAdViewRequested", LOG_TAG)
            if (SharedBase.Ads.isActive().or(SmartNotify.isLoggedIn().not()))
                return
            if (SharedBase.Ads.isTrailActive())
                return
            ads?.loadAd(AdRequest.Builder().build())
        }

        fun linkAdView(adView: AdView) {
            SmartNotify.log("BannerAds::onAdViewLinked", LOG_TAG)
            this.ads = adView
            ads?.visibility = View.GONE
            ads?.adListener = listener
        }

        fun linkAdView(activity: Activity, id: Int) {
            SmartNotify.log("BannerAds::onAdViewLinked", LOG_TAG)
            this.ads = activity.findViewById(id)
            ads?.visibility = View.GONE
            ads?.adListener = listener
        }

        fun unlinkAdView() {
            SmartNotify.log("BannerAds::onAdViewUnlinked", LOG_TAG)
            ads = null
        }
    }

    class AppOpenAds {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        private var isShowingAd = false
        private var loadTime: Long = 0
        private fun wasLoadTimeLessThanNHoursAgo(hours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < (numMilliSecondsPerHour * hours)
        }

        fun isAdShowing(): Boolean {
            return isShowingAd
        }

        fun isAdAvailable(): Boolean {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(2)
        }

        fun requestNewAd(context: Context) {
            if (SharedBase.Ads.isActive().or(SmartNotify.isLoggedIn().not()).or(SharedBase.Ads.isTrailActive()))
                return
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                context.getString(R.string.ad_id_open_app),
                request,
                //AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        // Called when an app open ad has loaded.
                        SmartNotify.log("AppOpenAds::onAdLoaded", LOG_TAG)
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Called when an app open ad has failed to load.
                        SmartNotify.log(
                            "AppOpenAds::onAdFailedToLoad:${loadAdError.message}",
                            LOG_TAG
                        )
                        isLoadingAd = false
                    }
                })
        }

        fun display(activity: Activity): Boolean {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                SmartNotify.log("AppOpenAds::display():Ad Already Showing", LOG_TAG)
                return true
            }

            if (!isAdAvailable()) {
                SmartNotify.log("AppOpenAds::display():Ad not ready", LOG_TAG)
                requestNewAd(activity)
                return false
            }

            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    // Called when full screen content is dismissed.
                    // Set the reference to null so isAdAvailable() returns false.
                    SmartNotify.log("AppOpenAds::display():onAdDismissedFullScreenContent", LOG_TAG)
                    appOpenAd = null
                    isShowingAd = false
                    requestNewAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when fullscreen content failed to show.
                    // Set the reference to null so isAdAvailable() returns false.
                    SmartNotify.log(
                        "AppOpenAds::display():onAdFailedToShowFullScreenContent:${adError.message}",
                        LOG_TAG
                    )
                    appOpenAd = null
                    isShowingAd = false
                    requestNewAd(activity)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    SmartNotify.log("AppOpenAds::display():onAdClicked", LOG_TAG)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    SmartNotify.log("AppOpenAds::display():onAdImpression", LOG_TAG)
                }

                override fun onAdShowedFullScreenContent() {
                    SmartNotify.log("AppOpenAds::display():onAdShowedFullScreenContent", LOG_TAG)
                }
            }
            isShowingAd = true
            if (SharedBase.Ads.isActive().or(SmartNotify.isLoggedIn().not()).or(SharedBase.Ads.isTrailActive()))
                return false
            appOpenAd?.show(activity)
            return true
        }
    }

    class InterstitialAds {
        private var mInterstitialAd: InterstitialAd? = null
        private var isLoadingAd = false
        private var isShowingAd = false

        private fun isAdAvailable(): Boolean {
            return mInterstitialAd != null
        }

        fun requestNewAd(context: Context) {
            if (SharedBase.Ads.isActive().or(SmartNotify.isLoggedIn().not()).or(SharedBase.Ads.isTrailActive()))
                return

            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            InterstitialAd.load(context,
                context.getString(R.string.ad_id_interstitial),
                request,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        SmartNotify.log(
                            "InterstitialAds::requestNewAd():onAdFailedToLoad:${adError.message}",
                            LOG_TAG
                        )
                        isLoadingAd = false
                        mInterstitialAd = null
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        SmartNotify.log("InterstitialAds::requestNewAd():onAdLoaded", LOG_TAG)
                        isLoadingAd = false
                        mInterstitialAd = interstitialAd
                    }
                })
        }

        fun display(activity: Activity): Boolean {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                SmartNotify.log("InterstitialAds::display():Ad Already Showing", LOG_TAG)
                return true
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (isAdAvailable().not()) {
                SmartNotify.log("InterstitialAds::display():Ad not ready", LOG_TAG)
                requestNewAd(activity)
                return false
            }
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    // Called when full screen content is dismissed.
                    // Set the reference to null so isAdAvailable() returns false.
                    SmartNotify.log(
                        "InterstitialAds::display():onAdDismissedFullScreenContent",
                        LOG_TAG
                    )
                    mInterstitialAd = null
                    isShowingAd = false
                    requestNewAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when fullscreen content failed to show.
                    // Set the reference to null so isAdAvailable() returns false.
                    SmartNotify.log(
                        "InterstitialAds::display():onAdFailedToShowFullScreenContent:${adError.message}",
                        LOG_TAG
                    )
                    mInterstitialAd = null
                    isShowingAd = false
                    requestNewAd(activity)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    SmartNotify.log("InterstitialAds::display():onAdClicked", LOG_TAG)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    SmartNotify.log("InterstitialAds::display():onAdImpression", LOG_TAG)
                }

                override fun onAdShowedFullScreenContent() {
                    SmartNotify.log(
                        "InterstitialAds::display():onAdShowedFullScreenContent",
                        LOG_TAG
                    )
                }
            }
            isShowingAd = true
            if (SharedBase.Ads.isActive()
                    .or(SmartNotify.isLoggedIn().not())
                    .or(SharedBase.Ads.isTrailActive())
            ) return false

            mInterstitialAd?.show(activity)
            return true
        }
    }

    class RewardAds {
        private var rewardedAd: RewardedAd? = null
        private var isLoadingAd = false
        private var isShowingAd = false

        private fun isAdAvailable(): Boolean {
            return rewardedAd != null
        }

        fun requestNewAd(context: Context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            RewardedAd.load(context,
                context.getString(R.string.ad_id_reward),
                request,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        SmartNotify.log(
                            "RewardAds::requestNewAd():onAdFailedToLoad:${adError.message}",
                            LOG_TAG
                        )
                        isLoadingAd = false
                        rewardedAd = null
                        //requestNewAd(context)
                    }

                    override fun onAdLoaded(p0: RewardedAd) {
                        super.onAdLoaded(p0)
                        SmartNotify.log("RewardAds::requestNewAd():onAdLoaded", LOG_TAG)
                        isLoadingAd = false
                        rewardedAd = p0
                    }

                })
        }

        fun display(activity: Activity, rewardListener: () -> Unit): Boolean {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                SmartNotify.log("RewardAds::display():Ad Already Showing", LOG_TAG)
                return true
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (isAdAvailable().not()) {
                SmartNotify.log("RewardAds::display():Ad not ready", LOG_TAG)
                requestNewAd(activity)
                return false
            }
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    // Called when full screen content is dismissed.
                    // Set the reference to null so isAdAvailable() returns false.
                    SmartNotify.log(
                        "RewardAds::display():onAdDismissedFullScreenContent",
                        LOG_TAG
                    )
                    rewardedAd = null
                    isShowingAd = false
                    requestNewAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when fullscreen content failed to show.
                    // Set the reference to null so isAdAvailable() returns false.
                    SmartNotify.log(
                        "RewardAds::display():onAdFailedToShowFullScreenContent:${adError.message}",
                        LOG_TAG
                    )
                    rewardedAd = null
                    isShowingAd = false
                    requestNewAd(activity)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    SmartNotify.log("RewardAds::display():onAdClicked", LOG_TAG)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    SmartNotify.log("RewardAds::display():onAdImpression", LOG_TAG)
                }

                override fun onAdShowedFullScreenContent() {
                    SmartNotify.log(
                        "RewardAds::display():onAdShowedFullScreenContent",
                        LOG_TAG
                    )
                }
            }
            isShowingAd = true
            rewardedAd?.show(
                activity
            ) { p0 ->
                SmartNotify.log(
                    "RewardAds::display():OnUserEarnedRewardListener::${p0.amount}",
                    LOG_TAG
                )
                rewardListener()
            }
            return true
        }

        fun hasAd(): Boolean {
            return isAdAvailable()
        }
    }
}