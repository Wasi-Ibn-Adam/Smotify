package com.lassanit.authkit.handler

import android.content.Context
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import com.lassanit.extras.fragments.FragmentHandler
import com.lassanit.extras.classes.Anime

class AuthFragmentHandler(
    context: Context,
    manager: FragmentManager,
    stackNumber: Int,
    @IdRes containerId: Int,
    listener: FragmentChangeListener? = null
) : FragmentHandler(context, manager, stackNumber, containerId, listener) {
    private var anime: Anime? = null
    private var popAnime: Anime? = null

    override fun handleBackPress(): Boolean {
        if (list.size <= 1) return false
        pop()
        return true
    }

    override fun setFragmentAnime(anime: Anime) {
        this.anime = anime
    }

    override fun setPopAnime(anime: Anime) {
        this.popAnime = anime
    }

}