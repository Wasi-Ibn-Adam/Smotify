package com.lassanit.authkit.activities

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.AdditionalUserInfo
import com.google.firebase.auth.OAuthProvider
import com.lassanit.authkit.fragments.PhoneFragment
import com.lassanit.authkit.fragments.ResetFragment
import com.lassanit.authkit.fragments.SignInFragment
import com.lassanit.authkit.fragments.SignUpFragment
import com.lassanit.authkit.fragments.SocialFragment
import com.lassanit.authkit.handler.AuthFragmentHandler
import com.lassanit.authkit.handler.GoogleSsoHandler
import com.lassanit.authkit.handler.ProviderSsoHandler
import com.lassanit.authkit.interfaces.AuthActions
import com.lassanit.extras.WaitingDialog
import com.lassanit.extras.classes.Anime
import com.lassanit.extras.classes.Utils
import com.lassanit.extras.fragments.FragmentHandler
import com.lassanit.firekit.R
import com.lassanit.kit.FireKitCompatActivity.Companion.options

class AuthActivity : AppCompatActivity(), AuthActions {
    companion object {
        fun getInstance(context: Context): Intent {
            return Intent(
                context,
                AuthActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    private lateinit var topNav: AuthFragmentHandler
    private lateinit var btmNav: AuthFragmentHandler
    private lateinit var popUp: WaitingDialog

    private lateinit var googleSsoHandler: GoogleSsoHandler

    private lateinit var parent: View
    private lateinit var topFrame: FrameLayout
    private lateinit var btmFrame: FrameLayout

    private var backPress = true
    private var first = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_authkit)
        setActivityView()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED)

        //loadFrags
        topNav = AuthFragmentHandler(this, supportFragmentManager, 0, R.id.top_frame)
        topNav.setApp(options.getApp())
            .setDesign(options.getDesign())
        topNav.setFragmentAnime(Anime())
        topNav.addFragmentChangeListener(object : FragmentHandler.FragmentChangeListener() {
            override fun onChange(name: String?, attached: Boolean) {
                try {
                    log("$name : $attached")
                    if (name == null)
                        return
                    when (name) {
                        SignInFragment::class.java.simpleName -> {
                            if (btmFrame.visibility == GONE) {
                                // only when split screen was on (was in split mode but now full screen)
                                btmFrame.visibility = VISIBLE
                                requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                            btmNav.show(SocialFragment::class.java)

                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })

        if (options.getAuth().currentUser != null)
            onSignInComplete()
        else
            topNav.push(SignInFragment(true))

        btmNav = AuthFragmentHandler(this, supportFragmentManager, 1, R.id.btm_frame)
        btmNav.setApp(options.getApp())
            .setCompany(options.getCompany())
            .setDesign(options.getDesign())
        btmNav.setFragmentAnime(Anime(R.anim.show, R.anim.hide))
        btmNav.push(SocialFragment(), delayMillis = 500)
        googleSsoHandler = GoogleSsoHandler.make(this, this)
        popUp = WaitingDialog(this)


        onBackPressedDispatcher.addCallback(
            this /* lifecycle owner */,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (backPress) {
                        if (!topNav.handleBackPress()) {
                            finishAndRemoveTask()
                        }
                    }
                }
            })
    }

    private fun setActivityView() {
        parent = findViewById(R.id.include)
        topFrame = findViewById(R.id.top_frame)
        btmFrame = findViewById(R.id.btm_frame)

        if (options.getDesign()?.activity != null) {
            parent.setBackgroundResource(options.getDesign()?.activity!!.background)
            parent.background.alpha = 124
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (options.getAuth().currentUser != null)
                onSignInComplete()
        } catch (e: Exception) {
            e.printStackTrace()
            options.getCallBacks().onFailure(e)
        }
    }

    override fun log(any: Any) {
        Log.d(options.getTag(), any.toString())
    }

    override fun onForgetClick() {
        topNav.push(ResetFragment(), SignInFragment::class.java)
    }

    override fun onRegisterClick() {
        topNav.push(SignUpFragment(), SignInFragment::class.java)
    }

    override fun loginWithProvider(provider: OAuthProvider) {
        try {
            ProviderSsoHandler(this, provider, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onFacebookClick() {
        try {
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onGoogleClick() {
        googleSsoHandler.onClick()
    }

    override fun onPhoneClick() {
        try {
            val frag = topNav.getCurr()
            if (frag != null)
                topNav.push(PhoneFragment(), frag::class.java)
            else topNav.push(PhoneFragment())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onFragmentLoaded(str: String, view: View) {
        log(" onFragmentLoaded : $str ")
        if (str == ResetFragment::class.java.simpleName || str == PhoneFragment::class.java.simpleName) {
            btmFrame.visibility = GONE
        }
    }

    override fun onFragmentComplete(str: String) {
        log(" onFragmentComplete : $str ")
        if (str == ResetFragment::class.java.simpleName || str == PhoneFragment::class.java.simpleName) {
            btmFrame.visibility = VISIBLE
        }
    }

    override fun backPress(allow: Boolean) {
        backPress = allow
    }

    override fun loadingPopup(show: Boolean) {
        if (show)
            popUp.show()
        else
            popUp.dismiss()
    }

    override fun onResetLinkSent() {
        Toast.makeText(
            this,
            "Password Reset link is sent to your email.",
            Toast.LENGTH_SHORT
        ).show()
        topNav.pop()
    }

    override fun onSignInComplete() {
        options.getCallBacks().onSignIn(false)
        overridePendingTransition(R.anim.show, R.anim.hide)
    }

    override fun onSignUpComplete(signInMethod: Utils.SignInMethod) {
        topNav.pop()
        options.getCallBacks().onSignUp()
        overridePendingTransition(R.anim.show, R.anim.hide)
        finish()
    }

    override fun onAdditionalInformation(info: AdditionalUserInfo) {
        try {
            first = info.isNewUser
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

