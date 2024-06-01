package com.lassanit.smotify.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lassanit.smotify.R
import com.lassanit.smotify.activities.HomeActivity
import com.lassanit.smotify.activities.NotificationActivity
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.display.adapter.AppHeaderAdapter
import com.lassanit.smotify.display.adapter.ItemSwiper
import com.lassanit.smotify.display.data.AppHeader
import com.lassanit.smotify.display.holder.AppHeaderHolder
import com.lassanit.smotify.handlers.LockManager
import com.lassanit.smotify.interfaces.ExternalImps
import com.lassanit.smotify.interfaces.InternalImps
import com.lassanit.smotify.popups.AppDialog
import com.lassanit.smotify.services_receivers.SmartPhoneService

class HomeNotificationFragment : Fragment(), InternalImps.HomeNavMenuFragImp,
    AppHeaderAdapter.Callbacks,
    ExternalImps.SmartCloudServiceImp, ExternalImps.SmartPhoneServiceImp {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_notification, container, false
        )
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppHeaderAdapter
    private lateinit var headerSelectionL: LinearLayout
    private lateinit var deleteB: AppCompatButton
    private lateinit var readB: AppCompatButton
    private lateinit var visibilityB: AppCompatButton
    private lateinit var cancelB: AppCompatButton
    private var reloadPkg: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            recyclerView = view.findViewById(R.id.app_recycler)
            headerSelectionL = view.findViewById(R.id.app_header_selection)
            headerSelectionL.visibility = View.GONE
            deleteB = view.findViewById(R.id.app_header_selection_delete)
            readB = view.findViewById(R.id.app_header_selection_read)
            visibilityB = view.findViewById(R.id.app_header_selection_visiblity)
            cancelB = view.findViewById(R.id.app_header_selection_cancel)


            recyclerView.layoutManager = LinearLayoutManager(view.context)
            adapter = AppHeaderAdapter(requireActivity())
            recyclerView.adapter = adapter

            adapter

            cancelB.setOnClickListener {
                runCatching {
                    adapter.selectionEnd()
                    headerSelectionL.visibility = View.GONE
                }
            }
            deleteB.setOnClickListener {
                runCatching {
                    val pop = AppDialog.needPermission.ask(
                        requireActivity(),
                        "Delete Data",
                        "All Notifications of selected Application will be deleted...",
                        "Delete All"
                    )
                    pop.set({
                        LockManager(requireActivity(), {
                            adapter.deleteSelected()
                            cancelB.callOnClick()
                        }, null).show()
                    }, null)
                    pop.show()
                }
            }
            readB.setOnClickListener {
                runCatching {
                    adapter.readSelected()
                    cancelB.callOnClick()
                }
            }
            visibilityB.setOnClickListener {
                runCatching {
                    LockManager(requireActivity(), {
                        adapter.toggleVisibilitySelected()
                        cancelB.callOnClick()
                    }, null).show()

                }
            }
        }
        runCatching { handleSwipe() }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            if (reloadPkg != null) {
                adapter.readHeader(reloadPkg!!)
                reloadPkg = null
            }
        }.onFailure { it.printStackTrace() }
        runCatching {
            AppHeaderAdapter.connect(this)
            HomeActivity.connect(this)
            SmartPhoneService.connect(this)
            SmartPhoneService.connectCloud(this)
        }
    }

    override fun onPause() {
        super.onPause()
        runCatching {
            HomeActivity.disconnect()
            AppHeaderAdapter.disconnect()
        }
        runCatching {
            cancelB.callOnClick()
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching {
            SmartPhoneService.disconnectCloud()
            SmartPhoneService.disconnect()
        }
    }

    override fun onMsgAdded(pkg: String) {
        runCatching {
            if (adapter.isCloudType())
                return@runCatching
            val i = adapter.addHeader(pkg) ?: return
            recyclerView.scrollToPosition(i)
        }
    }

    override fun onMsgUpdated(pkg: String, mid: Int) {
        runCatching { onMsgAdded(pkg) }
    }

    override fun hasOldMsg() {
        runCatching {
            if (adapter.isCloudType())
                return@runCatching
            adapter.dataReload()
        }
    }

    override fun onCloudMsgReceived(pkg: String) {
        runCatching {
            if (adapter.isCloudType().not())
                return@runCatching
            val i = adapter.addHeader(pkg) ?: return
            recyclerView.scrollToPosition(i)
        }
    }

    override fun onNavigationAllRead() {
        runCatching { adapter.readHeaders() }
        runCatching { cancelB.callOnClick() }
    }

    override fun onNavigationVisibilityToggle() {
        runCatching {
            adapter = AppHeaderAdapter(requireActivity())
            recyclerView.adapter = adapter
        }.onFailure { it.printStackTrace() }
        runCatching { cancelB.callOnClick() }
    }

    private fun handleSwipe() {
        ItemSwiper.onlySwipe(
            ItemTouchHelper.RIGHT,
            callBacks = object : ItemSwiper.CallBacks {
                override fun onSwipeStart(holder: RecyclerView.ViewHolder) {}

                override fun onSwipeComplete(holder: RecyclerView.ViewHolder) {
                    if (holder is AppHeaderHolder) {
                        adapter.readHeader(adapter.getItem(holder.adapterPosition) as AppHeader)
                    }
                }
            }).attachToRecyclerView(recyclerView)
    }

    override fun onItemSelectionActions(
        isActionAllowed: Boolean,
        hasUnread: Boolean,
        isSecretAllowed: Boolean,
    ) {
        headerSelectionL.visibility = View.VISIBLE

        (deleteB.visibility) = if (isActionAllowed) View.VISIBLE else View.GONE
        (visibilityB.visibility) = if (isActionAllowed) {
            if (isSecretAllowed) View.VISIBLE else View.GONE
        } else View.GONE
        (readB.visibility) = if (isActionAllowed) {
            if (hasUnread) View.VISIBLE else View.GONE
        } else View.GONE

        visibilityB.text =
            if (SharedBase.Visibility.Phone.isNormalNotifications()) "Secret" else "General"
    }

    override fun onAppHeaderDetailRequired(pkg: String, isCloud: Boolean) {
        runCatching {
            reloadPkg = pkg
            startActivity(NotificationActivity.getInstance(requireContext(), pkg, isCloud.not()))
        }.onFailure { it.printStackTrace() }
    }

}