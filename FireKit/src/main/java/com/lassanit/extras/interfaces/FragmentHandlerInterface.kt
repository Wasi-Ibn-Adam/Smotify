package com.lassanit.extras.interfaces

import android.view.View
import com.lassanit.authkit.fragments.AuthFragment
import com.lassanit.extras.classes.Anime
import com.lassanit.extras.fragments.FragmentHandler


interface FragmentHandlerInterface {

    fun push(f: AuthFragment, _class: Class<*>, delayMillis: Long = 0)
    fun push(f: AuthFragment, map: HashMap<Int, View>? = null, delayMillis: Long = 0)
    fun pop()
    fun popPrev()
    fun clear()

    fun handleBackPress(): Boolean

    fun addFragmentChangeListener(listener: FragmentHandler.FragmentChangeListener)

    fun setFragmentAnime(anime: Anime)
    fun setPopAnime(anime: Anime)

    fun hide(_class: Class<*>)
    fun show(_class: Class<*>)

    fun getFragment(_class: Class<*>): AuthFragment
    fun getCurr(): AuthFragment?

}