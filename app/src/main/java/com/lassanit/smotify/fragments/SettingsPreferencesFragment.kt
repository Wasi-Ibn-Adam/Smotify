package com.lassanit.smotify.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lassanit.smotify.R
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.customviews.SettingsTab
import com.lassanit.smotify.handlers.ServiceManager

class SettingsPreferencesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_setting_backups, container, false
        )
    }

    private lateinit var mediaClickT: SettingsTab
    private lateinit var mediaShowT: SettingsTab
    private lateinit var mediaSaveT: SettingsTab
    private lateinit var msgScrollT: SettingsTab
    private lateinit var batterT: SettingsTab

   // private val permissionDialog = AppDialog.needPermission.resultPermissions.makeBattery(requireActivity() as AppCompatActivity)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            mediaClickT = view.findViewById(R.id.tab_s_media_click)
            mediaSaveT = view.findViewById(R.id.tab_s_media_save)
            mediaShowT = view.findViewById(R.id.tab_s_media_show)
            msgScrollT = view.findViewById(R.id.tab_s_msg_scroll)
            batterT = view.findViewById(R.id.tab_s_battery)
        }.onFailure { it.printStackTrace() }
        runCatching {
            mediaClickT.onCheckClickListener { SharedBase.Settings.Edit.toggleMediaClickInAppHeaderAllowed() }
            mediaSaveT.onCheckClickListener { SharedBase.Settings.Edit.toggleMediaSavingAllowed() }
            mediaShowT.onCheckClickListener { SharedBase.Settings.Edit.toggleMediaShowInAppHeaderAllowed() }
            msgScrollT.onCheckClickListener { SharedBase.Settings.Edit.toggleMessageAddedScrollAllowed() }
        }.onFailure { it.printStackTrace() }
        //runCatching {
        //    permissionDialog.create({
        //        batterT.visibility = View.GONE
        //    })
        //    batterT.setOnClickListener { permissionDialog.show() }
        //}.onFailure { it.printStackTrace() }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            mediaShowT.setCheck(SharedBase.Settings.Display.isMediaShowInAppHeaderAllowed())
            mediaClickT.setCheck(SharedBase.Settings.Display.isMediaClickInAppHeaderAllowed())
            mediaSaveT.setCheck(SharedBase.Settings.Display.isMediaSavingAllowed())
            msgScrollT.setCheck(SharedBase.Settings.Display.isMessageAddedScrollAllowed())
            msgScrollT.setCheck(SharedBase.Settings.Display.isMessageAddedScrollAllowed())
        }
        runCatching {
            if (ServiceManager.isIgnoringBatteryOptimizations(requireContext()))
                batterT.visibility = View.GONE
        }.onFailure { it.printStackTrace() }
    }
}