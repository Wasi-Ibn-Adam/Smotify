package com.lassanit.smotify.handlers

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import de.hdodenhof.circleimageview.CircleImageView

class StorageManager(activity: FragmentActivity) {
    private var uri: Uri? = null
    private var storageHandlerImp: StorageHandlerImp? = null
    private lateinit var imgI: ImageView

    private val pickMedia =
        activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            runCatching{
                this.uri = uri
                if (uri != null) {
                    runCatching { storageHandlerImp?.onUriAvailable(uri) }.onFailure { it.printStackTrace() }
                    if (::imgI.isInitialized) {
                        imgI.setImageURI(uri)
                    }
                }
            }.onFailure { it.printStackTrace() }
        }


    private val pickDoc =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data
                if (uri != null) {
                    runCatching{ storageHandlerImp?.onUriAvailable(uri) }
                }
            }
        }

    fun connect(imageHandlerImp: StorageHandlerImp) {
        this.storageHandlerImp = imageHandlerImp
    }


    fun into(view: ImageView) {
        imgI = view
        if (uri != null)
            view.setImageURI(uri)
    }

    fun into(view: CircleImageView) {
        imgI = view
        if (uri != null)
            view.setImageURI(uri)
    }

    fun loadDocument(imageHandlerImp: StorageHandlerImp) {
        this.storageHandlerImp = imageHandlerImp
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            type = "application/octet-stream"
        }
        pickDoc.launch(intent)
    }

    fun makeDocument() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, "database.le")
        }
        pickDoc.launch(intent)
    }

    fun loadImage() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }


    fun loadImageOrVideo() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    fun loadVideo() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    fun loadGif() {
        val mimeType = "image/gif"
        pickMedia.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.SingleMimeType(
                    mimeType
                )
            )
        )
    }

    interface StorageHandlerImp {
        fun onUriAvailable(uri: Uri)
    }

}