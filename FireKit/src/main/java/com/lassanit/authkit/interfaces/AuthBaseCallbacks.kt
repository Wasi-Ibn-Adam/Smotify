package com.lassanit.authkit.interfaces

import com.google.firebase.auth.AdditionalUserInfo
import com.lassanit.extras.classes.Utils

sealed interface AuthBaseCallbacks : AuthSocialCallbacks {
    fun onResetLinkSent()
    fun onSignInComplete()
    fun onSignUpComplete(signInMethod: Utils.SignInMethod)
    fun onAdditionalInformation(info: AdditionalUserInfo)
}