package com.lassanit.smotify.display.holder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lassanit.smotify.R
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.interfaces.InternalImps

class AppHolder(private val v: View) : RecyclerView.ViewHolder(v) {
    companion object {
        fun getHolder(parent: ViewGroup): AppHolder {
            return AppHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.holder_row_app, parent, false)
            )
        }

        private var imp: InternalImps.HomeAppImp? = null
        fun connect(imp: InternalImps.HomeAppImp) {
            this.imp = imp
        }

        fun disconnect() {
            imp = null
        }

    }

    private val name: TextView = v.findViewById(R.id.holder_row_app_name)
    private val logo: ImageView = v.findViewById(R.id.holder_row_app_img)


    private fun loadLogo(pkg: String) {
        val draw =
            runCatching {
                v.context.packageManager.getApplicationIcon(pkg)
            }.getOrElse { ResourcesCompat.getDrawable(v.resources, R.drawable.base_removed, null) }
        runCatching { Glide.with(v).load(draw).into(logo) }
    }

    private fun viewActive(active: Boolean) {
        if (!active) logo.imageAlpha = 50
        else logo.imageAlpha = 255
    }

    fun handle(pos: Int, app: App) {
        name.text = app.name
        loadLogo(app.pkg)
        viewActive(SharedBase.Apps.isAppActive(app.pkg))
        logo.setOnClickListener {
            imp?.onAppClick(pos, it, app)
        }
        logo.setOnLongClickListener {
            imp?.onAppLongClick(pos, it, app)
            true
        }
        v.setOnLongClickListener {
            imp?.onAppLongClick(pos, it, app)
            true
        }
    }

}