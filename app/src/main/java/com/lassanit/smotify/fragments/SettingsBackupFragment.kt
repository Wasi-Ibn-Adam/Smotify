package com.lassanit.smotify.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lassanit.smotify.R
import com.lassanit.smotify.customviews.SettingsTab
import com.lassanit.smotify.handlers.StorageManager
import com.lassanit.smotify.popups.AppDialog

class SettingsBackupFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_setting_backups, container, false
        )
    }

    private lateinit var backupT: SettingsTab
    private lateinit var backupCT: SettingsTab
    private lateinit var restoreT: SettingsTab
    private lateinit var restoreCT: SettingsTab
    private val storageManager = StorageManager(requireActivity())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            backupT = view.findViewById(R.id.tab_s_backup)
            restoreT = view.findViewById(R.id.tab_s_restore)
        }.onFailure { it.printStackTrace() }
        runCatching {
            backupT.setOnClickListener {
                val alert = AppDialog.needPermission.ask(
                    requireActivity(),
                    "Backup",
                    buildString {
                        append("All Notifications, Media and Saved List of Applications will be saved in this backup")
                    },
                    "Continue"
                )
                alert.set({
                    alert.dismiss()

                }, null)
                alert.show()
            }
            restoreT.setOnClickListener {
                val alert = AppDialog.needPermission.ask(
                    requireActivity(),
                    "Restore Local-Backup",
                    buildString {
                        append("All Notifications, Media and Saved List of Applications will be merge with existing")
                    },
                    "Continue"
                )
                alert.set({

                }, null)
                alert.show()
            }

        }.onFailure { it.printStackTrace() }

    }


}