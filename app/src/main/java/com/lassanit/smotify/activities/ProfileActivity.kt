package com.lassanit.smotify.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.lassanit.extras.WaitingDialog
import com.lassanit.extras.classes.Utils
import com.lassanit.smotify.R
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.activities.ProfileActivity.Companion.ClassValues.HOME
import com.lassanit.smotify.activities.ProfileActivity.Companion.ClassValues.SETTINGS
import com.lassanit.smotify.bases.CloudBase
import com.lassanit.smotify.bases.CloudTaskImp
import com.lassanit.smotify.customviews.ProfileTab
import com.lassanit.smotify.handlers.StorageManager
import com.lassanit.smotify.popups.AppDialog
import com.lassanit.smotify.popups.AppInputDialog
import com.lassanit.smotify.popups.InputDialogImp
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream


class ProfileActivity : AppCompatActivity(), StorageManager.StorageHandlerImp, InputDialogImp {
    companion object {
        fun getInstance(context: Context): Intent {
            return Intent(context, ProfileActivity::class.java)
        }

        private var userUpdate: Int = 0

        private object ClassValues {
            const val HOME = 1
            const val SETTINGS = 2
        }

        fun userUpdated() {
            userUpdate = (HOME).or(SETTINGS)
        }

        fun updateUsed(className: String) {
            when (className) {
                HomeActivity::class.java.name -> {
                    userUpdate = userUpdate and (HOME.inv())
                }

                SettingActivity::class.java.name -> {
                    userUpdate = userUpdate and (SETTINGS.inv())
                }
            }
        }

        fun isUserUpdated(className: String): Boolean {
            return when (className) {
                HomeActivity::class.java.name -> {
                    userUpdate.and(HOME) == HOME
                }

                SettingActivity::class.java.name -> {
                    userUpdate.and(SETTINGS) == SETTINGS
                }

                else -> {
                    false
                }
            }
        }
    }

    private val mediaHandler = StorageManager(this)

    private lateinit var editF: FloatingActionButton
    private lateinit var imgEditF: FloatingActionButton
    private lateinit var saveEditB: AppCompatButton

    private lateinit var imgC: CircleImageView
    private lateinit var nameT: ProfileTab
    private lateinit var emailT: ProfileTab
    private lateinit var passT: ProfileTab

    private lateinit var providerL: LinearLayout

    private lateinit var changeDialog: AppInputDialog

    private lateinit var wait: WaitingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        editF = findViewById(R.id.profile_edit)
        imgEditF = findViewById(R.id.profile_edit_img)
        saveEditB = findViewById(R.id.profile_edit_save)

        imgC = findViewById(R.id.profile_img)

        nameT = findViewById(R.id.profile_t_name)
        emailT = findViewById(R.id.profile_t_email)
        passT = findViewById(R.id.profile_t_pass)
        providerL = findViewById(R.id.profile_t_provider)

        wait = WaitingDialog(this)

        mediaHandler.connect(this)
        editF.setOnClickListener {
            nameT.showEdit(true)
            editF.visibility = View.GONE
            imgEditF.visibility = View.VISIBLE
            saveEditB.visibility = View.VISIBLE
        }

        nameT.onValueEditClickListener {
            runCatching {
                changeDialog = AppDialog.needInput
                    .makeName(this, nameT.getValue(), this)
                changeDialog.show()
            }
        }

        saveEditB.setOnClickListener {
            userUpdated()
            nameT.showEdit(false)
            editF.visibility = View.VISIBLE
            imgEditF.visibility = View.GONE
            saveEditB.visibility = View.GONE
        }

        imgEditF.setOnClickListener { mediaHandler.loadImage() }

        imgC.setOnClickListener {
            if (editF.isVisible.not()) return@setOnClickListener
            val path = Firebase.auth.currentUser?.photoUrl
            if (path != null)
                AppDialog.needDisplay.make(this, path, false).show()
        }

        passT.onLabelClickListener {
            changeDialog = AppDialog.needInput.makePassword(this, this)
            changeDialog.show()
        }

        val providers = Firebase.auth.currentUser?.providerData
        if (providers.isNullOrEmpty()) return

        val basePID = Utils.getProviderData(Utils.SignInMethod.EMAIL)!!.providerId

        for (id in providers) {
            SmartNotify.log(buildString {
                append("providerId\t").append(id.providerId).appendLine()
                append("displayName\t").append(id.displayName).appendLine()
                append("email\t").append(id.email).appendLine()
                append("photoUrl\t").append(id.photoUrl).appendLine()
                append("uid\t").append(id.uid).appendLine()
                append("isEmailVerified\t").append(id.isEmailVerified).appendLine()
                append("phoneNumber\t").append(id.phoneNumber)
            }, "pROVIDER iD")
            val provider = Utils.getProviderData(id.providerId) ?: continue

            if (provider.providerId != basePID) {
                providerL.visibility = View.VISIBLE
                passT.visibility = View.GONE
            } else continue

            val img = AppCompatImageView(this)
            img.setImageResource(provider.appRes)
            providerL.addView(img, 50, 50)
        }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            val user = Firebase.auth.currentUser ?: return@runCatching
            nameT.setValue(user.displayName ?: " SN-1${user.uid.take(7)}")
            val email = user.email
            if (email.isNullOrBlank()) {
                val phone = user.phoneNumber
                if (phone.isNullOrBlank()) {
                    emailT.visibility = View.GONE
                } else {
                    emailT.setLabel("Phone no:")
                    emailT.setValue(phone)
                }
            } else {
                emailT.setValue(email)
            }

            Glide.with(this).load(user.photoUrl)
                .placeholder(R.drawable.base_user)
                .into(imgC)
        }
    }

    override fun onUriAvailable(uri: Uri) {
        runCatching {
            wait.show()
            Glide.with(this).asBitmap()
                .load(uri)
                .placeholder(R.drawable.person)
                .addListener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        wait.dismiss()
                        return true
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        runCatching {
                            val stream = ByteArrayOutputStream()
                            resource?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            upload(byteArray)
                        }.onFailure { e -> e.printStackTrace(); wait.dismiss() }
                        return true
                    }
                }).submit()
        }.onFailure { it.printStackTrace(); wait.dismiss() }
    }

    private fun upload(byteArray: ByteArray) {
        runCatching {
            CloudBase.Storage.uploadProfileImage(
                Firebase.auth.currentUser?.uid!!,
                byteArray,
                object : CloudTaskImp {
                    override fun onSuccess(any: Any) {
                        updateUserImage(any as Uri)
                    }

                    override fun onFailure(e: Exception?) {
                        wait.dismiss()
                    }
                })
        }.onFailure { it.printStackTrace(); wait.dismiss() }
    }

    private fun updateUserImage(uri: Uri) {
        runCatching {
            val builder = UserProfileChangeRequest.Builder()
            builder.photoUri = uri
            Firebase.auth.currentUser
                ?.updateProfile(builder.build())
                ?.addOnCompleteListener {
                    if (it.isSuccessful) {
                        Glide.with(this@ProfileActivity).load(uri)
                            .placeholder(R.drawable.base_user)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    wait.dismiss()
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    wait.dismiss()
                                    return false
                                }
                            })
                            .into(imgC)
                    } else
                        wait.dismiss()
                }
        }.onFailure { it.printStackTrace() }
    }

    override fun setPassword(curr: String, new: String) {
        runCatching {
            wait.show()
            val user = Firebase.auth.currentUser!!
            val credential = EmailAuthProvider.getCredential(user.email!!, curr)
            user.reauthenticate(credential).addOnCompleteListener {
                if (it.isSuccessful) {
                    user.updatePassword(new).addOnCompleteListener { it1 ->
                        wait.dismiss()
                        changeDialog.dismiss()
                        if (it1.isSuccessful) {
                            Toast.makeText(this, "Password Changed", Toast.LENGTH_SHORT).show()
                        } else {
                            it1.exception?.printStackTrace()
                            Toast.makeText(
                                this,
                                "Password Changing Failed: ${it1.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    wait.dismiss()
                    it.exception?.printStackTrace()
                    Toast.makeText(
                        this,
                        "Authentication Failed: ${it.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }.onFailure {
            changeDialog.dismiss()
            wait.dismiss();
            it.printStackTrace()
        }
    }

    override fun setName(name: String) {
        runCatching {
            wait.show()
            val builder = UserProfileChangeRequest.Builder()
            builder.displayName = name
            Firebase.auth.currentUser?.updateProfile(builder.build())
                ?.addOnCompleteListener {
                    if (it.isSuccessful) {
                        nameT.setValue(name)
                    }
                    wait.dismiss()
                }
        }.onFailure { wait.dismiss();it.printStackTrace() }
    }

}