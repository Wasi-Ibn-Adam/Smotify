package com.lassanit.extras.fragments

import android.content.Context
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE
import com.lassanit.authkit.fragments.AuthFragment
import com.lassanit.extras.classes.App
import com.lassanit.extras.classes.Company
import com.lassanit.extras.classes.Designs
import com.lassanit.extras.interfaces.FragmentHandlerInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class FragmentHandler(
    private val context: Context,
    private val manager: FragmentManager,
    private val stackNumber: Int,
    @IdRes private val containerId: Int,
    private var listener: FragmentChangeListener? = null
) : FragmentHandlerInterface {
    class Frag(var id: Int, var name: String)

    private var app: App? = null
    private var company: Company? = null
    private var design: Designs? = null

    protected var list = ArrayList<Frag>()

    init {
        manager.addFragmentOnAttachListener { _: FragmentManager?, frag: Fragment ->
            listener?.onChange(
                frag.javaClass.simpleName,
                true
            )
        }
        manager.addOnBackStackChangedListener {
            try {
                val frag = manager.fragments[manager.fragments.size - 1]
                if (frag != null) listener?.onChange(frag.javaClass.simpleName, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setDesign(design: Designs?): FragmentHandler {
        this.design = design
        return this
    }

    fun setCompany(company: Company?): FragmentHandler {
        this.company = company
        return this
    }

    fun setApp(app: App): FragmentHandler {
        this.app = app
        return this
    }

    override fun push(f: AuthFragment, _class: Class<*>, delayMillis: Long) {
        try {
            push(f, getFragment(_class).getDefaultLinker(), delayMillis)
        } catch (e: Exception) {
            e.printStackTrace()
            push(f, null, delayMillis)
        }

    }


    override fun push(f: AuthFragment, map: HashMap<Int, View>?, delayMillis: Long) {
        if (app != null)
            f.setApp(app!!)
        if (company != null)
            f.setCompany(company!!)
        if (design != null)
            f.setDesign(design!!)

        CoroutineScope(Dispatchers.Default).launch {
            delay(delayMillis)
            try {
                val transaction = manager.beginTransaction()
                transaction.setTransition(TRANSIT_FRAGMENT_FADE)

                if (map != null)
                    for (key in map.keys) {
                        val v = map[key] ?: continue
                        transaction.addSharedElement(v, context.getString(key))
                    }
                transaction.replace(containerId, f).addToBackStack(stackNumber.toString())
                val i = transaction.commit()
                list.add(Frag(i, f.javaClass.simpleName))
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun pop() {
        try {
            val frag = list.removeAt(list.size - 1)
            manager.popBackStack(frag.id, 1)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun popPrev() {
        try {
            val frag = list.removeAt(list.size - 2)
            if (frag.id >= 0) manager.popBackStack(frag.id, 1)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun clear() {
        manager.clearBackStack(stackNumber.toString())
    }

    override fun addFragmentChangeListener(listener: FragmentChangeListener) {
        this.listener = listener
    }

    override fun hide(_class: Class<*>) {
        try {
            val frag = getFragment(_class)
            if (frag.isAdded && frag.isVisible) {
                val transaction: FragmentTransaction = manager.beginTransaction()
                transaction.hide(frag).commit()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun show(_class: Class<*>) {
        try {
            val frag = getFragment(_class)
            if (frag.isAdded && frag.isHidden) {
                val transaction: FragmentTransaction = manager.beginTransaction()
                transaction.show(frag).commit()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun getFragment(_class: Class<*>): AuthFragment {
        return (manager.fragments.stream()
            .filter { fragment: Fragment? ->
                if (fragment == null)
                    false
                else
                    fragment.javaClass == _class
            }
            .findFirst()
            .orElse(null)) as AuthFragment
    }

    override fun getCurr(): AuthFragment? {
        val frag = list.lastOrNull()?:return null
        return (manager.fragments.stream()
            .filter { fragment: Fragment? -> fragment?.javaClass?.simpleName.toString() == (frag.name) }
            .findFirst()
            .orElse(null)) as AuthFragment
    }

    abstract class FragmentChangeListener {
        abstract fun onChange(name: String?, attached: Boolean)
    }

    fun size(): Int {
        return list.size
    }
}