package com.lassanit.smotify.popups

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.lassanit.smotify.R

class AppProgressDialog(activity: FragmentActivity) : AppDialog(activity) {
    private val txt: TextView
    private val img: ImageView

    init {
        setContentView(R.layout.dialog_progress)
        txt = findViewById(R.id.loading_txt)
        img = findViewById(R.id.videoView)
        runCatching {
            Glide.with(activity).asGif().load(com.lassanit.firekit.R.drawable.loader2)
                .placeholder(ColorDrawable(Color.LTGRAY)).into(img)
        }.onFailure { it.printStackTrace() }
        initFinalize()
    }

    fun setProgress(total: Int, cur: Int) {
        runCatching {
            txt.text = ((cur.times(100).div(total)).toString().plus("%"))
        }.onFailure { it.printStackTrace() }
    }

    fun setText(str: String) {
        runCatching {
            txt.text = str
        }.onFailure { it.printStackTrace() }
    }
}