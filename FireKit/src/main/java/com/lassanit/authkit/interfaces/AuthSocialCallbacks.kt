package com.lassanit.authkit.interfaces

import com.google.firebase.auth.OAuthProvider

sealed interface AuthSocialCallbacks{
    fun loginWithProvider(provider: OAuthProvider)
    fun onFacebookClick()
    fun onGoogleClick()
    fun onPhoneClick()
}