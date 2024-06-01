package com.lassanit.authkit.fragments

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.google.firebase.auth.OAuthProvider
import com.lassanit.extras.classes.Utils
import com.lassanit.extras.classes.Utils.SignInMethod.FACEBOOK
import com.lassanit.extras.classes.Utils.SignInMethod.GITHUB
import com.lassanit.extras.classes.Utils.SignInMethod.GOOGLE
import com.lassanit.extras.classes.Utils.SignInMethod.MICROSOFT
import com.lassanit.extras.classes.Utils.SignInMethod.PHONE
import com.lassanit.extras.classes.Utils.SignInMethod.TWITTER
import com.lassanit.extras.classes.Utils.SignInMethod.YAHOO
import com.lassanit.extras.customviews.SocialApp
import com.lassanit.firekit.R
import com.lassanit.kit.FireKitCompatActivity.Companion.options

class SocialFragment :
    AuthFragment(R.layout.fragment_auth_social) {
    private val signInMethods: Array<Utils.SignInMethod>? = options.getMethods()
    private lateinit var methodLayout: LinearLayout

    var enable: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actions.onFragmentLoaded(SocialFragment::class.java.simpleName, view)
        setupMethods(view)
    }

    override fun canAnimate(view: View) {
        super.canAnimate(view)
        view.startAnimation(Utils.getMoveAnimation(view.y))
    }

    private fun setupMethods(view: View) {
        view.findViewById<View>(R.id.layout_alter).visibility =
            if (signInMethods.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE

        methodLayout = view.findViewById(R.id.layout_methods)

        if (signInMethods != null) for (method in signInMethods) {
            methodLayout.addView(getMethodView(method))
        }
    }

    private fun getMethodView(signInMethod: Utils.SignInMethod): View? {
        val socialApp = SocialApp(requireContext())
        val providerData = Utils.getProviderData(signInMethod)?:return null
        socialApp.setImageResource(providerData.appRes)
        when (signInMethod) {
            PHONE -> {
                socialApp.setOnClickListener {
                    if (!enable) return@setOnClickListener
                    actions.onPhoneClick()
                }
            }
            TWITTER,
            MICROSOFT,YAHOO,
            GITHUB -> {
                socialApp.setOnClickListener {
                    if (!enable) return@setOnClickListener
                    val provider = OAuthProvider.newBuilder(providerData.providerId)
                    if (providerData.scopes != null)
                        provider.scopes = providerData.scopes
                    actions.loginWithProvider(provider.build())
                }
            }
            FACEBOOK -> {
                socialApp.setOnClickListener {
                    if (!enable) return@setOnClickListener
                    actions.onFacebookClick()
                }
            }
            GOOGLE -> {
                socialApp.setOnClickListener {
                    if (!enable) return@setOnClickListener
                    actions.onGoogleClick()
                }
            }
            else -> {
                return null
            }
        }
        socialApp.setText(signInMethod.name)
        return socialApp
    }

}