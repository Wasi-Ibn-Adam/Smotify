package com.lassanit.smotify.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lassanit.smotify.R
import com.lassanit.smotify.bases.CloudBaseHelper
import com.lassanit.smotify.bases.SmartBase
import com.lassanit.smotify.display.adapter.AppMessageChatterAdapter
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.handlers.ToolbarHandler
import com.lassanit.smotify.interfaces.ExternalImps
import com.lassanit.smotify.services_receivers.SmartPhoneService

class NotificationActivity : AppCompatActivity(), ExternalImps.SmartPhoneServiceImp {
    companion object {
        private const val PKG = "pkg"
        private const val DEVICE = "isDevice"
        fun getInstance(context: Context, pkg: String, isDevice: Boolean): Intent {
            return Intent(context, NotificationActivity::class.java).putExtra(PKG, pkg)
                .putExtra(DEVICE, isDevice)
        }
    }

    private lateinit var toolbarHandler: ToolbarHandler
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppMessageChatterAdapter
    private lateinit var db: SmartBase
    private lateinit var app: App
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(com.lassanit.firekit.R.style.Theme_FireKit_APP)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        runCatching {
            val isDevice = intent.getBooleanExtra(DEVICE, true)
            db = if (isDevice) SmartBase(this) else CloudBaseHelper.get(this)
        }.onFailure { it.printStackTrace() }
        runCatching {
            val pkg = intent.getStringExtra(PKG)
            if (pkg.isNullOrBlank()) {
                finish()
                return
            }
            app = db.displayHelper.getApp(pkg)!!
        }.onFailure { it.printStackTrace() }
        setupToolbar()
        // make all noti read
        runCatching {
            db.editHelper.readAppMessages(app.pkg)
        }
        setList()
    }

    private fun setupToolbar() {
        runCatching {
            toolbarHandler = ToolbarHandler.get(this, null, isUser = false)
            toolbarHandler.setTitle(app.name.plus(' '))
        }.onFailure { it.printStackTrace() }
    }

    private fun setList() {
        runCatching {
            recyclerView = findViewById(R.id.app_recycler)
            adapter = AppMessageChatterAdapter(this, app.pkg, db, null)
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
            recyclerView.adapter = adapter
            adapter.onScrollRequired = {
                recyclerView.smoothScrollToPosition(0)
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun onResume() {
        super.onResume()
        SmartPhoneService.connect(this, true)
    }

    override fun onPause() {
        super.onPause()
        SmartPhoneService.disconnect()
    }

    override fun onMsgAdded(pkg: String) {
        if (pkg == app.pkg) {
            adapter.messageAddedListUpdate()
        }
    }

    override fun onMsgUpdated(pkg: String, mid: Int) {
        if (pkg == app.pkg) {
            adapter.messageUpdatedUpdateList(mid)
        }
    }

    override fun hasOldMsg() {
        adapter.reload()
    }
}