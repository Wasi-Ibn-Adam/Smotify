package com.lassanit.smotify.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View.GONE
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.lassanit.smotify.R
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.ExportBase
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.bases.SharedBase.Settings.Display
import com.lassanit.smotify.bases.SharedBase.Settings.Edit
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.customviews.SettingsTab
import com.lassanit.smotify.handlers.ServiceManager
import com.lassanit.smotify.handlers.SmartAdsManager
import com.lassanit.smotify.handlers.StorageManager
import com.lassanit.smotify.handlers.ToolbarHandler
import com.lassanit.smotify.popups.AppDialog
import com.lassanit.smotify.popups.AppProgressDialog
import com.lassanit.smotify.popups.AppSubscriptionDialog

class SettingActivity : AppCompatActivity(),
    StorageManager.StorageHandlerImp {
    companion object {
        fun getInstance(context: Context): Intent {
            return Intent(context, SettingActivity::class.java)
        }
    }

    private lateinit var profile: SettingsTab

    private lateinit var mediaClickT: SettingsTab
    private lateinit var mediaShowT: SettingsTab
    private lateinit var mediaCount: SettingsTab
    private lateinit var mediaSaveT: SettingsTab
    private lateinit var msgScrollT: SettingsTab

    private lateinit var cloudLinkT: SettingsTab
    private lateinit var cloudShareT: SettingsTab
    private lateinit var backupT: SettingsTab
    private lateinit var restoreT: SettingsTab

    private lateinit var adRemovalT: SettingsTab
    private lateinit var adRewardT: SettingsTab

    private lateinit var policyT: SettingsTab
    private lateinit var termsT: SettingsTab

    private lateinit var joinUserT: SettingsTab
    private lateinit var logoutT: SettingsTab
    private lateinit var batterT: SettingsTab

    private lateinit var toolbarHandler: ToolbarHandler
    private val batteryPermission = AppDialog.needPermission.resultPermissions.makeBattery(this)

    private val storageManager = StorageManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        toolbarHandler = ToolbarHandler.get(this, isUser = true)
        profile = findViewById(R.id.tab_profile)

        mediaClickT = findViewById(R.id.tab_s_media_click)
        mediaCount = findViewById(R.id.tab_s_media_quick_count)
        mediaSaveT = findViewById(R.id.tab_s_media_save)
        mediaShowT = findViewById(R.id.tab_s_media_show)
        msgScrollT = findViewById(R.id.tab_s_msg_scroll)

        batterT = findViewById(R.id.tab_s_battery)

        cloudLinkT = findViewById(R.id.tab_s_cloud_link)
        cloudShareT = findViewById(R.id.tab_s_cloud_share)
        backupT = findViewById(R.id.tab_s_backup)
        restoreT = findViewById(R.id.tab_s_restore)

        adRemovalT = findViewById(R.id.tab_s_ads_removal)
        adRewardT = findViewById(R.id.tab_s_ads_removal_reward)

        policyT = findViewById(R.id.tab_s_policy)
        termsT = findViewById(R.id.tab_s_terms)

        logoutT = findViewById(R.id.tab_s_logout)
        joinUserT = findViewById(R.id.tab_s_join_user)



        runCatching {
            cloudShareT.onCheckClickListener { Edit.toggleCloudSharingAllowed() }
            mediaClickT.onCheckClickListener { Edit.toggleMediaClickInAppHeaderAllowed() }
            mediaSaveT.onCheckClickListener { Edit.toggleMediaSavingAllowed() }
            mediaShowT.onCheckClickListener { Edit.toggleMediaShowInAppHeaderAllowed() }
            msgScrollT.onCheckClickListener { Edit.toggleMessageAddedScrollAllowed() }
            mediaCount.onProgressListener {
                runCatching {
                    val txt = mediaCount.getDesc().toString().trim()
                    mediaCount.setDesc(txt.dropLastWhile { it1 -> it1 != '=' }.plus(' ').plus(it))
                    Edit.setMinMessagesInAppHeader(it)
                    //AppHeaderAdapter.notificationOpenReset()
                }.onFailure { it.printStackTrace() }
            }
            joinUserT.onTextClickListener {
                val alert = AppDialog.needPermission.ask(
                    this,
                    "Join as User",
                    buildString {
                        append("Your notifications remain secure when you switch to User mode, and you can ")
                        append("still access them. However, if you switch back to Guest mode, it's like ")
                        append("starting fresh, and your previous notifications won't be retained.")
                    },
                    "Continue"
                )
                alert.set({ SmartNotify.onLogout(this) }, null)
                alert.show()
            }
            logoutT.onTextClickListener {
                val alert = AppDialog.needPermission.ask(
                    this,
                    "Confirmation",
                    buildString {
                        append("Your notifications remain secure if you login to with same Account, and you can ")
                        append("still access them. However, if you switch to Guest mode/or Different User Account, it's like ")
                        append("starting fresh, and your previous notifications won't be retained.").appendLine()
                        append("Are you sure, you want to logout?")
                    },
                    "Log-out"
                )
                alert.set({ SmartNotify.onLogout(this) }, null)
                alert.show()
            }
            profile.setOnClickListener {
                startActivity(ProfileActivity.getInstance(this))
            }
        }
        runCatching {
            policyT.onTextClickListener { startActivity(Resource.policyLink()) }
            termsT.onTextClickListener { startActivity(Resource.termsLink()) }
        }
        runCatching {
            batteryPermission.create({
                batterT.visibility = GONE
            })
            batterT.setOnClickListener { batteryPermission.show() }
        }.onFailure { it.printStackTrace() }
        runCatching {
            cloudLinkT.onTextClickListener(getCloudLinkListener())
            adRemovalT.onTextClickListener(getAdListener())

            if (SmartNotify.isGuest()) {
                logoutT.visibility = GONE
                joinUserT.visibility = VISIBLE
            } else {
                logoutT.visibility = VISIBLE
                joinUserT.visibility = GONE
            }
        }
        runCatching {
            backupT.setOnClickListener {
                val alert = AppDialog.needPermission.ask(
                    this,
                    "Backup",
                    buildString {
                        append("All Notifications, Media and Saved List of Applications will be saved in this backup")
                    },
                    "Continue"
                )
                alert.set({
                    runCatching {
                        alert.dismiss()
                        val waitingDialog = AppProgressDialog(this)
                        ExportBase(this).export(
                            Firebase.auth.currentUser?.uid.toString(),
                            onStart = { runOnUiThread { waitingDialog.show() } },
                            onComplete = { runOnUiThread { waitingDialog.dismiss() } },
                            onProgressDetail = { runOnUiThread { waitingDialog.setText(it) } },
                            onSuccess = {
                                runOnUiThread {
                                    if (it !== null) {
                                        val path = Uri.parse(it)
                                        Snackbar.make(
                                            this@SettingActivity.window.decorView,
                                            "Backup is Complete",
                                            Snackbar.LENGTH_SHORT
                                        ).setAction("Check") {
                                            val intent = Intent(Intent.ACTION_VIEW)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            intent.setDataAndType(path, "*/*")
                                            startActivity(intent)
                                        }.show()
                                    } else {
                                        Snackbar.make(
                                            this@SettingActivity.window.decorView,
                                            "Backup is Complete",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }.onFailure {
                        it.printStackTrace()
                        alert.dismiss()
                    }
                }, null, false).show()
            }
            restoreT.setOnClickListener {
                val alert = AppDialog.needPermission.ask(
                    this,
                    "Restore Local-Backup",
                    buildString {
                        append("All Notifications, Media and Saved List of Applications will be merge with existing")
                    },
                    "Continue"
                )
                alert.set({
                    alert.dismiss()
                    storageManager.loadDocument(this)
                }, null, false).show()
            }
        }.onFailure { it.printStackTrace() }
        if (SmartNotify.isGuest()) {
            profile.visibility = GONE
            mediaCount.visibility = GONE
        }
        adRewardT.onTextClickListener {
            SmartAdsManager.requestRewardDisplay(this) {
                runCatching {
                    SharedBase.Ads.earnOneHour()
                    this.recreate()
                }.onFailure { it.printStackTrace() }
            }
        }
    }

    private fun getAdListener(): OnClickListener? {
        return if (SharedBase.Ads.isActive()) {
            adRemovalT.setDesc("Removed till : ${Resource.getTimeOrDate(SharedBase.Ads.getTime())}")
            adRemovalT.setTitle(null)
            null
        } else if (SharedBase.Ads.isTrailActive()) {
            adRemovalT.setDesc("Trial till : ${Resource.getTimeOrDate(SharedBase.Ads.getTrailTime())}")
            adRemovalT.setTitle(null)
            null
        } else {
            adRemovalT.setTitle(null)
            if (SmartNotify.isGuest()) {
                adRemovalT.setDesc("Guest can not subscribe to any Ads Removal Offer.")
                null
            } else {
                adRemovalT.setDesc("Subscribe & Remove Now!")
                OnClickListener {
                    AppSubscriptionDialog.makeAds(this) {
                        adRemovalT.setOnClickListener(getAdListener())
                    }.show()
                }
            }
        }
    }

    private fun getCloudLinkListener(): OnClickListener? {
        return if (SharedBase.CloudService.isActive()) {
            cloudLinkT.setImage(R.drawable.base_linked_c)
            cloudLinkT.setDesc("Linked till: ${Resource.getTimeOrDate(SharedBase.CloudService.getTime())}")
            cloudLinkT.setTitle(null)
            cloudShareT.isEnabled = true
            null
        } else {
            cloudLinkT.setImage(R.drawable.base_link_c)
            cloudLinkT.setTitle(null)
            cloudShareT.isEnabled = false
            if (SmartNotify.isGuest()) {
                cloudLinkT.setDesc("Guest can not use Cloud services.")
                null

            } else {
                cloudLinkT.setDesc("Link account to Cloud Services for sharing Notifications across devices.")
                OnClickListener {
                    AppSubscriptionDialog.makeCloud(this) {
                        cloudLinkT.setOnClickListener(getCloudLinkListener())
                    }.show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            toolbarHandler.checkRefreshNeeded(SettingActivity::class.java.name)
            mediaCount.progress(Display.getMinMessagesInAppHeader())
            cloudShareT.setCheck(Display.isCloudSharingAllowed())
            mediaShowT.setCheck(Display.isMediaShowInAppHeaderAllowed())
            mediaClickT.setCheck(Display.isMediaClickInAppHeaderAllowed())
            mediaSaveT.setCheck(Display.isMediaSavingAllowed())
            msgScrollT.setCheck(Display.isMessageAddedScrollAllowed())
            msgScrollT.setCheck(Display.isMessageAddedScrollAllowed())
        }
        runCatching {
            if (ServiceManager.isIgnoringBatteryOptimizations(this))
                batterT.visibility = GONE
        }.onFailure { it.printStackTrace() }
        runCatching {
            adRewardT.visibility = if (SmartAdsManager.hasRewardAd()) VISIBLE else GONE
        }.onFailure { it.printStackTrace() }
    }

    override fun onUriAvailable(uri: Uri) {
        SmartNotify.log(uri, "UriReceived")
        val waitingPop = AppProgressDialog(this@SettingActivity)
        ExportBase(this@SettingActivity)
            .importDB(
                Firebase.auth.currentUser?.uid.toString(),
                uri,
                onStart = { runOnUiThread { waitingPop.show() } },
                onComplete = {
                    runOnUiThread { waitingPop.dismiss() }
                    cacheDir.deleteRecursively()
                },
                onProgressDetail = {
                    SmartNotify.log(it, "onProgressDetail")
                    runOnUiThread { waitingPop.setText(it) }
                },
                onFailure = { it, _ ->
                    if (it.isNullOrBlank().not()) {
                        runOnUiThread {
                            Toast.makeText(
                                this@SettingActivity, it, Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onSuccess = {
                    runOnUiThread {
                        Toast.makeText(
                            this@SettingActivity,
                            "Backup is Restored, Restart the App, to check restored Notifications.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
    }
}