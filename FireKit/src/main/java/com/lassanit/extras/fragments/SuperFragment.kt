package com.lassanit.extras.fragments

import android.content.Context
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.lassanit.authkit.options.DesignHandler
import com.lassanit.extras.classes.App
import com.lassanit.extras.classes.Company
import com.lassanit.extras.classes.Designs
import com.lassanit.extras.interfaces.AppCallbacks
import com.lassanit.extras.interfaces.FragmentCallbacks
import com.lassanit.firekit.R


abstract class SuperFragment(@LayoutRes private var res: Int) : Fragment(), FragmentCallbacks {
    protected lateinit var designHandler: DesignHandler
    private lateinit var appCallbacks: AppCallbacks
    private var design: Designs? = null
    private var company: Company? = null
    protected var app: App? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AppCallbacks) {
            appCallbacks = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Set the shared element enter transition
        sharedElementEnterTransition = getTransition(
            requireContext(),
            android.R.transition.move
        )
        sharedElementReturnTransition = getTransition(
            requireContext(),
            android.R.transition.move
        )
        val view = inflater.inflate(res, container, false)
        canAnimate(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initApp(view)
        initCompany(view)
        if (::designHandler.isInitialized){
            initDesign(view)
        }
    }

    override fun hideView(parent: View, id: Int, gone: Boolean) {
        try {
            parent.findViewById<View>(id)?.visibility = if (gone) View.GONE else View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected fun getTransition(
        context: Context?, rid: Int,
        start: Runnable? = null,
        end: Runnable? = null,
        cancel: Runnable? = null,
        pause: Runnable? = null,
        resume: Runnable? = null
    ): Transition {
        return TransitionInflater.from(context).inflateTransition(rid)
            .apply {
                addListener(object : Transition.TransitionListener {
                    override fun onTransitionStart(transition: Transition) {
                        // Called when the transition starts
                        start?.run()
                    }

                    override fun onTransitionEnd(transition: Transition) {
                        // Called when the transition ends
                        end?.run()
                    }

                    override fun onTransitionCancel(transition: Transition) {
                        // Called when the transition is canceled
                        cancel?.run()
                    }

                    override fun onTransitionPause(transition: Transition) {
                        // Called when the transition is paused
                        pause?.run()
                    }

                    override fun onTransitionResume(transition: Transition) {
                        // Called when the transition is resumed
                        resume?.run()
                    }
                })
            }
    }

    override fun setDesign(design: Designs): SuperFragment {
        this.design = design
        designHandler = DesignHandler(design)
        return this
    }

    override fun setCompany(company: Company?): SuperFragment {
        this.company = company
        return this
    }

    override fun setApp(app: App): SuperFragment {
        this.app = app
        return this
    }


    override fun initApp(view: View) {
        try {
            if (app != null) {
                view.findViewById<ImageView>(R.id.fireKit_app_logo)?.setImageResource(app!!.res)
                view.findViewById<TextView>(R.id.fireKit_app_name)?.text = app!!.name
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun initCompany(view: View) {
        try {
            if (company != null) {
                view.findViewById<ImageView>(R.id.fireKit_company_logo)?.setImageResource(company!!.res)
                view.findViewById<TextView>(R.id.fireKit_company_name)?.text = company!!.name?.plus(" ")
            }
            view.findViewById<View>(R.id.fireKit_company)?.visibility =
                if (company == null) View.INVISIBLE else View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun getDefaultLinker(): HashMap<Int, View> {
        return HashMap()
    }

}