package com.lassanit.smotify.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.lassanit.authkit.options.AuthKitOptions
import com.lassanit.extras.classes.App
import com.lassanit.extras.classes.Company
import com.lassanit.extras.classes.Utils.SignInMethod
import com.lassanit.kit.FireKitCompatActivity
import com.lassanit.smotify.R
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.handlers.ServiceManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : FireKitCompatActivity(), AuthKitOptions.CallBacks {
    var isNew = false

    companion object {
        fun getInstance(context: Context): Intent {
            return Intent(
                context,
                SplashActivity::class.java
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun setOptions(): AuthKitOptions {
        return AuthKitOptions.Builder(
            FirebaseApp.getInstance(),
            App(getString(R.string.app_name), com.lassanit.firekit.R.drawable.app_logo)
        )
            .setCompany(Company("OneStop", com.lassanit.firekit.R.drawable.firekit_company_logo))
            .setCustomTag("SmartNotify")
            .setUserConsent(true)
            .enableGuestMode(true) //.enableSplashOnly(true)
            .setGoogleWebClientId(getString(R.string.default_web_client_id))
            .setCallBacks(this)
            .setSignInMethods(
                arrayOf(
                    SignInMethod.GOOGLE,
                    SignInMethod.PHONE,
                    SignInMethod.TWITTER
                )
            )
            .build()
    }

    override fun onSignIn(alreadySigned: Boolean) {
        runCatching { SmartNotify.setupCloudBase() }.onFailure { it.printStackTrace() }
        runCatching { MobileAds.initialize(this@SplashActivity) }.onFailure { it.printStackTrace() }
        runCatching {
            if (!alreadySigned) {
                SmartNotify.dbCheck(applicationContext)
                if (isNew && SmartNotify.isUser()) {
                    SharedBase.Ads.setTrail()
                }
            }
        }.onFailure { it.printStackTrace() }
        runCatching {
            Resource.saveAppList(applicationContext)
        }.onFailure { it.printStackTrace() }
        runCatching {
            startActivity(
                if (ServiceManager.isNLServiceEnabled(applicationContext))
                    HomeActivity.getInstance(this, isNew)
                else
                    PermissionActivity.getInstance(this, isNew)
            )
        }.onFailure { it.printStackTrace() }
    }

    override fun onSignUp() {
        isNew = true
        onSignIn(false)
    }

    override fun onEmailVerification(sent: Boolean) {}
    override fun onFailure(exception: Exception?) {
        exception?.let { Firebase.crashlytics.recordException(it) }
    }

    override fun onPolicy(): Intent {
        return Resource.policyLink()
    }

    override fun onTermsAndConditions(): Intent {
        return Resource.termsLink()
    }
}