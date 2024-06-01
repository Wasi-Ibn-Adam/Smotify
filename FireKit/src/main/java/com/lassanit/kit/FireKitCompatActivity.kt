package com.lassanit.kit

import com.lassanit.authkit.activities.FireSplashActivity
import com.lassanit.authkit.options.AuthKitOptions

abstract class FireKitCompatActivity : FireSplashActivity() {
    companion object{
        lateinit var options: AuthKitOptions
    }
}

