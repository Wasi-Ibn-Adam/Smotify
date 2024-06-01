package com.lassanit.smotify

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.lassanit.authkit.activities.AuthActivity
import com.lassanit.extras.WaitingDialog
import com.lassanit.smotify.activities.HomeActivity
import com.lassanit.smotify.activities.PermissionActivity
import com.lassanit.smotify.activities.ProfileActivity
import com.lassanit.smotify.activities.SplashActivity
import com.lassanit.smotify.bases.CloudBase
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.bases.SmartBase
import com.lassanit.smotify.handlers.ServiceManager
import com.lassanit.smotify.handlers.SmartAdsManager
import com.lassanit.smotify.handlers.SubscriptionManager
import com.lassanit.smotify.handlers.ToolbarHandlerImp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmartNotify : Application(), Application.ActivityLifecycleCallbacks {
    companion object {
        private lateinit var billingManager: SubscriptionManager
        private lateinit var pref: SharedPreferences
        private var toolbarHandlerImp: ToolbarHandlerImp? = null
        fun initPref(context: Context) {
            pref = context.getSharedPreferences(context.packageName, MODE_PRIVATE)
        }

        fun getPref(): SharedPreferences {
            return pref
        }

        fun setupCloudBase() {
            runCatching {
                log(
                    "setupCloudBase", "-SetupCloudBase"
                )
                val uid = Firebase.auth.currentUser?.uid ?: return
                log(
                    "setupCloudBase $uid", "-SetupCloudBase"
                )
                Firebase.crashlytics.setUserId(uid)
                Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
                CloudBase.RealTime.restoreAdSubscription(uid)
                CloudBase.RealTime.restoreCloudSubscription(uid)

            }.onFailure { e: Throwable ->
                e.printStackTrace()
                log(
                    "${e.message}  \n ${e.stackTrace}",
                    "-SetupCloudBase"
                )
            }
        }

        fun getSubscriptionManager(): SubscriptionManager {
            return billingManager
        }

        fun setupBillingCheck(context: Context) {
            billingManager = SubscriptionManager(context)
        }

        fun log(any: Any?, tag: String = "") {
            runCatching {
                Log.d("PeerLess-$tag", any.toString())
            }.onFailure { e: Throwable -> Firebase.crashlytics.recordException(e);e.printStackTrace() }
        }

        fun isGuest(): Boolean {
            val user = Firebase.auth.currentUser ?: return false
            return user.isAnonymous
        }

        fun isUser(): Boolean {
            val user = Firebase.auth.currentUser ?: return false
            return user.isAnonymous.not()
        }

        fun isLoggedIn(): Boolean {
            return (Firebase.auth.currentUser != null)
        }

        fun onLogout(activity: AppCompatActivity) {
            runCatching {
                val popup = WaitingDialog(activity)
                popup.show()
                getPref().edit().clear().apply()
                FirebaseAuth.getInstance().currentUser?.let {
                    CloudBase.Store.reset(it.uid)
                    SharedBase.setUser(it.uid)
                }
                runCatching { activity.cacheDir.deleteRecursively() }

                runCatching {
                    GoogleSignIn.getClient(activity, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
                }

                runCatching {
                    if (isGuest()) {
                        FirebaseAuth.getInstance().currentUser?.delete()
                            ?.addOnCompleteListener {
                                runCatching { FirebaseAuth.getInstance().signOut() }
                                popup.dismiss()
                                activity.startActivity(SplashActivity.getInstance(activity))
                            }
                    } else {
                        runCatching { FirebaseAuth.getInstance().signOut() }
                        popup.dismiss()
                        activity.startActivity(SplashActivity.getInstance(activity))
                    }
                }.onFailure { it.printStackTrace(); popup.dismiss() }
            }
        }

        fun dbCheck(context: Context) {
            runCatching {
                if (isLoggedIn()) {
                    val uid = Firebase.auth.currentUser?.uid
                    val lastUid = SharedBase.getLastUser()
                    if (uid.equals(lastUid).not()) {
                        context.deleteDatabase(SmartBase.DB_NAME)
                        //context.deleteDatabase(CloudBaseHelper.DB_NAME)
                        context.dataDir.deleteRecursively()
                        context.cacheDir.deleteRecursively()
                        context.obbDir.deleteRecursively()
                    }
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        runCatching { initPref(applicationContext) }.onFailure { it.printStackTrace() }
        runCatching { ServiceManager.setup(applicationContext) }.onFailure { it.printStackTrace() }
        runCatching {
            Firebase.initialize(this)
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
        }.onFailure { it.printStackTrace() }
        runCatching {
            registerActivityLifecycleCallbacks(this)
        }.onFailure { Firebase.crashlytics.recordException(it);it.printStackTrace() }
        runCatching {
            Firebase.appCheck.installAppCheckProviderFactory(
                if (BuildConfig.DEBUG)
                    DebugAppCheckProviderFactory.getInstance()
                else
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }.onFailure { e: Throwable -> Firebase.crashlytics.recordException(e);e.printStackTrace() }
        runCatching {
            Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
                runCatching {
                    Firebase.crashlytics.recordException(throwable)
                    throwable.printStackTrace()
                }.onFailure { it.printStackTrace() }
            }
        }.onFailure { it.printStackTrace() }
        runCatching {
            val billing = suspend {
                runCatching { setupBillingCheck(applicationContext) }.onFailure {
                    Firebase.crashlytics.recordException(it)
                    it.printStackTrace()
                }
            }
            val adsManager = suspend {
                runCatching {
                    SmartAdsManager.initialize(this@SmartNotify)
                }.onFailure {
                    Firebase.crashlytics.recordException(it)
                    it.printStackTrace()
                }
            }
            val userRefresh = suspend {
                runCatching {
                    Firebase.auth.currentUser?.let {
                        if (it.isAnonymous.not())
                            it.reload().addOnCompleteListener {
                                runCatching {
                                    ProfileActivity.userUpdated()
                                }
                            }
                    }
                }.onFailure {
                    Firebase.crashlytics.recordException(it)
                    it.printStackTrace()
                }
            }
            CoroutineScope(Dispatchers.IO).launch { adsManager() }
            CoroutineScope(Dispatchers.IO).launch { billing() }
            CoroutineScope(Dispatchers.IO).launch { userRefresh() }
        }.onFailure { it.printStackTrace() }
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        runCatching {
            if (p0::class.java == HomeActivity::class.java) {
                SharedBase.Visibility.reset()
                SmartAdsManager.requestAppOpenAd(p0)
                SmartAdsManager.requestRewardLoadOnly(p0)
            }
        }.onFailure { e: Throwable -> Firebase.crashlytics.recordException(e);e.printStackTrace() }
    }

    override fun onActivityStarted(p0: Activity) {
        runCatching {
            when (p0::class.java) {
                SplashActivity::class.java,
                AuthActivity::class.java,
                PermissionActivity::class.java
                -> {
                    return
                }
            }
            SmartAdsManager.requestInterstitialAd(p0)
        }.onFailure { e: Throwable -> Firebase.crashlytics.recordException(e);e.printStackTrace() }
    }

    override fun onActivityResumed(p0: Activity) {
        runCatching {
            toolbarHandlerImp?.onUserUpdated(Firebase.auth.currentUser)
        }.onFailure { Firebase.crashlytics.recordException(it);it.printStackTrace() }
        runCatching {
            SmartAdsManager.linkBannerAds(p0)
            SmartAdsManager.requestBannerAds()
        }.onFailure { Firebase.crashlytics.recordException(it);it.printStackTrace() }
    }

    override fun onActivityPaused(p0: Activity) {
        runCatching {
            SmartAdsManager.unlinkBannerAds()
        }.onFailure { Firebase.crashlytics.recordException(it);it.printStackTrace() }
    }

    override fun onActivityStopped(p0: Activity) {}

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

    override fun onActivityDestroyed(p0: Activity) {
        runCatching {
            if (p0::class.java == HomeActivity::class.java)
                cacheDir.deleteRecursively()
        }

    }

}