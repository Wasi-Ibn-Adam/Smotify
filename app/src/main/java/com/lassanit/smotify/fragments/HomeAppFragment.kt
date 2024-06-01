package com.lassanit.smotify.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lassanit.smotify.R
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.adapters.AdapterFragmentHomeApp
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.display.holder.AppHolder
import com.lassanit.smotify.handlers.ToolbarHandler
import com.lassanit.smotify.interfaces.ExternalImps
import com.lassanit.smotify.interfaces.InternalImps
import com.lassanit.smotify.popups.AppDialog
import com.lassanit.smotify.services_receivers.PhoneAppReceiver

class HomeAppFragment : Fragment(),
    ExternalImps.AppsImp,
    InternalImps.HomeAppImp,
    InternalImps.AppNavigationImp {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app, container, false)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayout: LinearLayout
    private lateinit var adapter: AdapterFragmentHomeApp

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            linearLayout = view.findViewById(R.id.empty_lay)
            recyclerView = view.findViewById(R.id.app_recycler)
            recyclerView.layoutManager = GridLayoutManager(view.context, 3)
            adapter = AdapterFragmentHomeApp(view.context)
            recyclerView.adapter = adapter
            onNavigationModeToggle(SharedBase.PhoneService.isActive())
        }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            PhoneAppReceiver.connect(this)
            AppHolder.connect(this)
            ToolbarHandler.connect(this)
        }
    }

    override fun onPause() {
        super.onPause()
        runCatching {
            PhoneAppReceiver.disconnect()
            AppHolder.disconnect()
            ToolbarHandler.disconnect()
        }
    }

    override fun onPhoneAppAdded(pkg: String) {
        runCatching { adapter.appAdded(pkg) }.onFailure { it.printStackTrace() }
    }

    override fun onPhoneAppRemoved(pkg: String) {
        runCatching { adapter.appRemoved(pkg) }.onFailure { it.printStackTrace() }
    }

    override fun onPhoneAppUpdated(pkg: String) {
        runCatching { adapter.appUpdated(pkg) }.onFailure { it.printStackTrace() }
    }

    override fun hasPreviousData() {
        runCatching {
            adapter.reload()
        }.onFailure { it.printStackTrace() }
    }

    override fun onAppClick(pos: Int, view: View, app: App) {
        runCatching {
            if (SmartNotify.isGuest()) {
                Toast.makeText(
                    requireContext(),
                    "Guest can not block any App Notifications",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                SharedBase.Apps.toggleAppState(app.pkg)
                adapter.notifyItemChanged(pos)
            }
        }
    }

    override fun onAppLongClick(pos: Int, view: View, app: App) {
        runCatching {
            AppDialog.needMenu.makeApplication(requireActivity(), view)
                .show(app, object : InternalImps.HomeAppItemImp {
                    override fun onItem(on: Boolean) {
                        onAppClick(pos, view, app)
                    }
                })
        }
    }

    override fun onNavigationModeToggle(state: Boolean) {
        runCatching {
            linearLayout.visibility = if (!state) VISIBLE else GONE
            recyclerView.visibility = if (state) VISIBLE else GONE
        }
    }
}
