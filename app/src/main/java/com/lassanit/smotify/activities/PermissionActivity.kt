package com.lassanit.smotify.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lassanit.firekit.R
import com.lassanit.smotify.popups.AppDialog

class PermissionActivity : AppCompatActivity() {
    companion object {
        fun getInstance(context: Context, isNew: Boolean): Intent {
            return Intent(context, PermissionActivity::class.java)
                .putExtra("isNew", isNew)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    private val notificationPermissionDialog =
        AppDialog.needPermission.resultPermissions.makeNoti(this)

    private var isNew = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isNew = intent.getBooleanExtra("isNew", false)
        runCatching {
            notificationPermissionDialog.create({
                runCatching {
                    notificationPermissionDialog.dismiss()
                    startActivity(HomeActivity.getInstance(this, isNew))
                    overridePendingTransition(R.anim.show, R.anim.hide)
                    finish()
                }.onFailure { it.printStackTrace() }
            }, {
                notificationPermissionDialog.dismiss()
                finishAndRemoveTask()
            })
        }.onFailure { it.printStackTrace() }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            notificationPermissionDialog.show()
        }.onFailure { it.printStackTrace() }
    }

    override fun onPause() {
        super.onPause()
        runCatching {
            notificationPermissionDialog.dismiss()
        }.onFailure { it.printStackTrace() }
    }

}