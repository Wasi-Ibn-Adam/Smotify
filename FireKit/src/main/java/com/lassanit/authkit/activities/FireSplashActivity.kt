package com.lassanit.authkit.activities

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lassanit.authkit.options.AuthKitOptions
import com.lassanit.authkit.options.PolicyDialog
import com.lassanit.authkit.options.PolicyImp
import com.lassanit.authkit.options.UserModeDialog
import com.lassanit.extras.WaitingDialog
import com.lassanit.firekit.R
import com.lassanit.kit.FireKitCompatActivity.Companion.options

@SuppressLint("CustomSplashScreen")
abstract class FireSplashActivity : AppCompatActivity() {

    abstract fun setOptions(): AuthKitOptions

    private lateinit var modeDialog: UserModeDialog
    private lateinit var waitingDialog: WaitingDialog
    private lateinit var policyDialog: PolicyDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        options = setOptions()
        waitingDialog = WaitingDialog(this)
        policyDialog = PolicyDialog.make(
            this,
            options.getCallBacks(), object : PolicyImp {
                override fun onAgree() {
                    checkGuestMode()
                }

                override fun onError(e: Exception) {
                    e.printStackTrace()
                    options.getCallBacks().onFailure(e)
                }
            }
        )
        modeDialog = UserModeDialog(this)
        modeDialog.set(onGuest = {
            waitingDialog.show()
            options.getAuth().signInAnonymously().addOnCompleteListener {
                if (it.isSuccessful) {
                    modeDialog.dismiss(); workDone(true)
                }
                waitingDialog.dismiss()
            }
        }, onUser = { modeDialog.dismiss(); workDone(false) })
    }

    override fun onResume() {
        super.onResume()
        if (options.getConsent())
            policyDialog.showIfConsentRequired()
        else checkGuestMode()
    }

    private fun checkGuestMode() {
        if (options.isOnlySplash()) {
            workDone(guest = true, true)
            return
        }
        val user = options.getAuth().currentUser
        if (options.askGuestConsent()) {
            try {
                if (user == null) {
                    modeDialog.show()
                } else {
                    workDone(user.isAnonymous, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            workDone(false, user != null)
        }
    }

    private fun workDone(guest: Boolean, old: Boolean = false) {
        try {
            if (old)
                options.getCallBacks().onSignIn(true)
            else {
                if (guest) {
                    options.getCallBacks().onSignUp()
                } else {
                    startActivity(AuthActivity.getInstance(this))
                }
            }
            overridePendingTransition(R.anim.show, R.anim.hide)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}