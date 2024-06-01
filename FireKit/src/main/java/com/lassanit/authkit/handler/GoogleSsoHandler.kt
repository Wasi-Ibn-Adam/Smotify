package com.lassanit.authkit.handler

import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.GoogleAuthProvider
import com.lassanit.authkit.interfaces.AuthActions
import com.lassanit.extras.classes.Utils
import com.lassanit.kit.FireKitCompatActivity.Companion.options

class GoogleSsoHandler(
    private val activity: AppCompatActivity,
    private val authActions: AuthActions
) {
    private val oneTapClient: SignInClient = Identity.getSignInClient(activity)
    private val request: BeginSignInRequest =
        BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(options.getWebClientId()!!).build()
        )
            .build()
    private var launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            handleResult(result)
        }

    private fun handleResult(result: ActivityResult) {
        if (result.resultCode != AppCompatActivity.RESULT_OK) {
            Toast.makeText(activity, "Google Sign-In Canceled", Toast.LENGTH_SHORT).show()
            return
        }
        authActions.loadingPopup(true)
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val authCredential = GoogleAuthProvider.getCredential(credential.googleIdToken, null)
            options.getAuth().signInWithCredential(authCredential)
                .addOnSuccessListener { authResult ->
                    if (authResult.user != null) {
                        Toast.makeText(
                            activity,
                            "Google Sign-In Success",
                            Toast.LENGTH_SHORT
                        ).show()
                        authActions.loadingPopup(false)
                        authResult.additionalUserInfo?.let { authActions.onAdditionalInformation(it) }
                        if (authResult.additionalUserInfo?.isNewUser == true)
                            authActions.onSignUpComplete(Utils.SignInMethod.GOOGLE)
                        else
                            authActions.onSignInComplete()
                    }
                }.addOnFailureListener {
                    Toast.makeText(activity, "Google Sign-In Failed", Toast.LENGTH_SHORT)
                        .show()
                    authActions.loadingPopup(false)
                }
        } catch (e: ApiException) {
            authActions.loadingPopup(false)
            when (e.statusCode) {
                CommonStatusCodes.CANCELED -> authActions.log("One-tap dialog was closed.")
                CommonStatusCodes.NETWORK_ERROR ->
                    authActions.log("One-tap encountered a network error.")

                else -> authActions.log("Couldn't get credential from result." + e.localizedMessage)
            }
        } catch (e: Exception) {
            authActions.loadingPopup(false)
            authActions.log("One-tap error: " + e.localizedMessage)
        }
    }

    fun onClick() {
        try {
            authActions.loadingPopup(true)
            oneTapClient.beginSignIn(request)
                .addOnSuccessListener { beginSignInResult ->
                    try {
                        authActions.log("One Tap beginSignIn.")
                        val intentSenderRequest =
                            IntentSenderRequest.Builder(
                                beginSignInResult.pendingIntent.intentSender
                            ).build()
                        launcher.launch(intentSenderRequest)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    authActions.loadingPopup(false)
                }
                .addOnFailureListener { e1 ->
                    authActions.log("One Tap beginSignIn: ERROR: " + e1.message)
                    if (e1.localizedMessage != null && e1.localizedMessage
                            .equals(
                                "16: Caller has been temporarily blocked due to too many canceled sign-in prompts.",
                                true
                            )
                    ) {
                        Toast.makeText(
                            activity,
                            "Too many Wrong Attempts, try again later.",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        e1.printStackTrace()
                    }
                    authActions.loadingPopup(false)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            authActions.loadingPopup(false)
        }
    }

    companion object {
        fun make(activity: AppCompatActivity, authActions: AuthActions): GoogleSsoHandler {
            return GoogleSsoHandler(activity, authActions)
        }
    }
}