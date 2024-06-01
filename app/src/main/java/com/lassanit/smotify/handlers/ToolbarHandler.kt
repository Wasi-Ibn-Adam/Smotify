package com.lassanit.smotify.handlers

import android.graphics.drawable.Drawable
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.get
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.lassanit.smotify.R
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.activities.ProfileActivity
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.interfaces.InternalImps

interface ToolbarHandlerImp {
    fun onUserUpdated(user: FirebaseUser?)
}

open class ToolbarHandler(
    private val activity: AppCompatActivity,
    private val imp: InternalImps.ToolBarImp?,
    id: Int
) : ToolbarHandlerImp {
    companion object {
        fun get(
            activity: AppCompatActivity,
            imp: InternalImps.ToolBarImp? = null,
            id: Int = R.id.handler_toolbar,
            isUser: Boolean = true,
            isEmail: Boolean = false,
        ): ToolbarHandler {
            return ToolbarHandler(activity, imp, id).setUser(isUser, isEmail)
        }

        private var imp: InternalImps.AppNavigationImp? = null
        fun connect(imp: InternalImps.AppNavigationImp) {
            this.imp = imp
        }

        fun disconnect() {
            imp = null
        }
    }

    private val toolBar: Toolbar = activity.findViewById(id)
    private var user: FirebaseUser? = Firebase.auth.currentUser
    private var isEmail: Boolean = false
    private var isUser: Boolean = false

    protected fun setUser(isUser: Boolean, isEmail: Boolean = false): ToolbarHandler {
        this.isEmail = isEmail
        this.isUser = isUser
        setUser()
        return this
    }

    private fun setUser() {
        runCatching {
            if (isUser.not())
                return
            when (SmartNotify.isGuest()) {
                true -> toolBar.title = "Guest"
                else -> {
                    toolBar.title = user?.displayName
                    if (isEmail)
                        toolBar.subtitle = user?.email ?: user?.phoneNumber
                }
            }
            Glide.with(activity).asDrawable()
                .circleCrop()
                .load(user?.photoUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        runCatching {
                            toolBar.navigationIcon = resource
                        }
                        return true
                    }
                })
                .submit(80, 80)
        }
    }

    override fun onUserUpdated(user: FirebaseUser?) {
        this.user = user
        setUser()
    }

    fun checkRefreshNeeded(className: String) {
        if (ProfileActivity.isUserUpdated(className)) {
            onUserUpdated(Firebase.auth.currentUser)
            ProfileActivity.updateUsed(className)
        }
    }

    fun setMenu() {
        runCatching {
            toolBar.inflateMenu(R.menu.home_nav_bar)
            toolBar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.nav_toggle -> {
                        SharedBase.PhoneService.toggleState()
                        menuIconToggleUpdate(item)
                    }

                    R.id.nav_popup -> {
                        imp?.onMenuExtend(toolBar)
                    }

                    else -> {}
                }
                true
            }
            for (i in (0 until toolBar.menu.size())) {
                val item = toolBar.menu[i]
                when (item.itemId) {
                    R.id.nav_toggle -> {
                        menuIconToggleUpdate(item)
                    }

                    else -> {
                        menuIconTint(item.icon)
                    }
                }
            }
        }
    }

    private fun menuIconTint(icon: Drawable?) {
        if (icon != null) {
            DrawableCompat.setTint(
                icon,
                ContextCompat.getColor(
                    activity,
                    com.lassanit.firekit.R.color.fireKit_opposite_theme
                )
            )
        }
    }

    private fun menuIconToggleUpdate(item: MenuItem) {
        val state = SharedBase.PhoneService.isActive()
        item.setIcon(if (state.not()) R.drawable.base_bell_no else R.drawable.base_bell_1)
        Companion.imp?.onNavigationModeToggle(state)
    }

    fun setToggle(pos: Int) {
        runCatching {
            if (toolBar.menu[0].itemId == R.id.nav_toggle)
                toolBar.menu[0].isVisible = (pos == HomeNavigationHandler.POS_APPS)
        }
    }

    fun setTitle(string: String) {
        runCatching {
            toolBar.title = string
        }
    }

    fun setSubTitle(string: String) {
        runCatching {
            toolBar.subtitle = string
        }
    }
}