package com.lassanit.authkit.handler

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.OAuthProvider
import com.lassanit.authkit.interfaces.AuthActions
import com.lassanit.kit.FireKitCompatActivity.Companion.options

class ProviderSsoHandler(activity: AppCompatActivity,provider: OAuthProvider,authActions: AuthActions) {
    init {
        try {
            options.getAuth().startActivityForSignInWithProvider(activity, provider)
                .addOnCompleteListener { task ->
                    authActions.log(
                        "loginWithProvider " + provider.providerId + " startActivityForSignInWithProvider: COMPLETE"
                    )
                    if (task.isSuccessful && task.result.user != null) {
                        authActions.log(
                            "loginWithProvider " + provider.providerId + " startActivityForSignInWithProvider: SUCCESS"
                        )
                        task.result.additionalUserInfo?.let { authActions.onAdditionalInformation(it) }
                    }
                }
                .addOnFailureListener { e ->
                    if (e is FirebaseAuthUserCollisionException) {
                        Toast.makeText(
                            activity,
                            "This email is Registered with different Sign-In Provider." +
                                    " Sign in using a provider associated with this email address",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        authActions.log(
                            "loginWithProvider " + provider.providerId + " startActivityForSignInWithProvider: ERROR: " + e.message
                        )
                        options.getAuth().signOut()
                        options.getCallBacks().onFailure(e)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}