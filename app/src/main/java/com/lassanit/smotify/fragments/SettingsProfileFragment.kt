package com.lassanit.smotify.fragments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import com.lassanit.smotify.activities.ProfileActivity
import com.lassanit.smotify.bases.CloudBase
import com.lassanit.smotify.bases.CloudTaskImp
import com.lassanit.smotify.customviews.ProfileTab
import com.lassanit.smotify.handlers.StorageManager
import com.lassanit.smotify.popups.AppDialog
import com.lassanit.smotify.popups.AppInputDialog
import com.lassanit.smotify.popups.InputDialogImp
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

class SettingsProfileFragment : Fragment(), StorageManager.StorageHandlerImp, InputDialogImp {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setting_profile, container, false)
    }

//    private val mediaHandler = StorageManager(requireActivity())

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            editF = view.findViewById(R.id.profile_edit)
            imgEditF = view.findViewById(R.id.profile_edit_img)
            saveEditB = view.findViewById(R.id.profile_edit_save)

            imgC = view.findViewById(R.id.profile_img)

            nameT = view.findViewById(R.id.profile_t_name)
            emailT = view.findViewById(R.id.profile_t_email)
            passT = view.findViewById(R.id.profile_t_pass)
            providerL = view.findViewById(R.id.profile_t_provider)

            wait = WaitingDialog(requireActivity() as AppCompatActivity)

            //mediaHandler.connect(this)
            editF.setOnClickListener {
                nameT.showEdit(true)
                editF.visibility = View.GONE
                imgEditF.visibility = View.VISIBLE
                saveEditB.visibility = View.VISIBLE
            }

            nameT.onValueEditClickListener {
                runCatching {
                    changeDialog = AppDialog.needInput
                        .makeName(requireContext(), nameT.getValue(), this)
                    changeDialog.show()
                }
            }

            saveEditB.setOnClickListener {
                ProfileActivity.userUpdated()
                nameT.showEdit(false)
                editF.visibility = View.VISIBLE
                imgEditF.visibility = View.GONE
                saveEditB.visibility = View.GONE
            }

           // imgEditF.setOnClickListener { mediaHandler.loadImage() }

            imgC.setOnClickListener {
                if (editF.isVisible.not()) return@setOnClickListener
                val path = Firebase.auth.currentUser?.photoUrl
                if (path != null)
                    AppDialog.needDisplay.make(view.context, path, false).show()
            }

            passT.onLabelClickListener {
                changeDialog = AppDialog.needInput.makePassword(view.context, this)
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

                val img = AppCompatImageView(requireContext())
                img.setImageResource(provider.appRes)
                providerL.addView(img, 50, 50)
            }
        }.onFailure { it.printStackTrace() }
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
                        Glide.with(this).load(uri)
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
                            Toast.makeText(requireContext(), "Password Changed", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            it1.exception?.printStackTrace()
                            Toast.makeText(
                                requireContext(),
                                "Password Changing Failed: ${it1.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    wait.dismiss()
                    it.exception?.printStackTrace()
                    Toast.makeText(
                        requireContext(),
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
