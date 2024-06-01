package com.lassanit.smotify.display.view

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore.Video.Thumbnails
import android.util.AttributeSet
import android.util.Size
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.lassanit.smotify.R
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.display.data.SmartMedia
import java.io.File
import java.util.Timer
import java.util.TimerTask


class SmartMediaView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : DataView(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    private var layImg: ConstraintLayout
    private var layAud: ConstraintLayout
    private var layVid: ConstraintLayout

    private var imgImg: ImageView

    private var audioImg: ImageView
    private var audioBar: SeekBar

    private var thumbImg: ImageView
    private var playB: AppCompatImageButton

    private var media: SmartMedia? = null

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var timer: Timer

    init {
        val view = inflate(getContext(), R.layout.display_view_data_msg_media, this)
        layAud = view.findViewById(R.id.media_lay_audio)
        layImg = view.findViewById(R.id.media_lay_img)
        layVid = view.findViewById(R.id.media_video)

        imgImg = view.findViewById(R.id.media_img)

        audioImg = view.findViewById(R.id.media_audio_icon)
        audioBar = view.findViewById(R.id.media_audio_bar)

        thumbImg = view.findViewById(R.id.media_thumb)
        playB = view.findViewById(R.id.media_play)

        audioBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                runCatching {
                    if (p2) {
                        if (::mediaPlayer.isInitialized) {

                            val pos = (mediaPlayer.duration / audioBar.max) * p1
                            SmartNotify.log(
                                "seek: ${mediaPlayer.duration}, ${mediaPlayer.currentPosition}, $p1, $pos  ",
                                "Player"
                            )

                            mediaPlayer.seekTo(pos)

                        }
                    }
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        this.visibility = GONE
        layAud.visibility = GONE
        layImg.visibility = GONE
        layVid.visibility = GONE
    }

    private fun setImage(bitmap: Bitmap?) {
        runCatching {
            if (bitmap == null)
                return
            layImg.visibility = VISIBLE
            Glide.with(this).asBitmap().load(bitmap).into(imgImg)
        }.onFailure { it.printStackTrace() }
    }

    private fun setImage(uri: Uri?) {
        runCatching {
            if (uri == null)
                return
            layImg.visibility = VISIBLE
            imgImg.setImageURI(uri)
        }.onFailure { it.printStackTrace() }
    }

    private fun setAudio(uri: Uri?) {
        runCatching {
            if (uri == null)
                return
            layAud.visibility = VISIBLE
            setupPlayer(uri)
            audioImg.setImageResource(R.drawable.base_play)
            audioImg.setOnClickListener {
                runCatching {
                    if (::mediaPlayer.isInitialized.not()) {
                        setupPlayer(uri)
                    }
                    if (mediaPlayer.isPlaying) {
                        audioImg.setImageResource(R.drawable.base_play)
                        mediaPlayer.pause()
                    } else {
                        audioImg.setImageResource(R.drawable.base_stop)
                        mediaPlayer.start()
                    }
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    private fun setVideo(uri: Uri?) {
        runCatching {
            if (uri == null) {
                return
            }
            layVid.visibility = VISIBLE

            playB.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setDataAndType(uri, "video/*")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
            thumbImg.setImageBitmap(createVideoThumbnail(uri))
        }.onFailure { it.printStackTrace() }
    }

    private fun createVideoThumbnail(uri: Uri): Bitmap? {
        return runCatching {
            uri.path?.let { File(it) }
                ?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ThumbnailUtils.createVideoThumbnail(it, Size(100, 100), null)
                    } else {
                        ThumbnailUtils.createVideoThumbnail(it.path, Thumbnails.MINI_KIND)
                    }
                }
        }.onFailure { it.printStackTrace() }.getOrNull()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        runCatching {
            resetPlayer()
        }
    }

    fun set(media: SmartMedia?) {
        runCatching {
            this.media = media
            if (media == null || media.exist().not()) {
                this.visibility = GONE
                return
            }
            this.visibility = VISIBLE

            layAud.visibility = GONE
            layImg.visibility = GONE
            layVid.visibility = GONE

            if (media.isImage(context)) {
                if (media.getMap() != null)
                    setImage(media.getMap())
                else
                    setImage(media.getUri())
            } else if (media.isAudio(context)) {
                setAudio(media.getUri())
            } else if (media.isVideo(context)) {
                setVideo(media.getUri())
            }
        }.onFailure { it.printStackTrace() }
    }

    private fun setupPlayer(uri: Uri) {
        runCatching {
            mediaPlayer = MediaPlayer()

            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()
            audioBar.max = 100

            timer = Timer()
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    runCatching {
                        if (mediaPlayer.isPlaying) {
                            val pos =
                                (mediaPlayer.currentPosition.toDouble() / mediaPlayer.duration) * 100
                            audioBar.progress = pos.toInt()
                        }
                    }.onFailure {
                        timer.cancel()
                        it.printStackTrace()
                    }
                }
            }, 0, 500)
            mediaPlayer.setOnCompletionListener {
                runCatching {
                    audioBar.progress = 100
                    timer.cancel()
                }
            }
        }
    }

    private fun resetPlayer() {
        runCatching {
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
            if (::timer.isInitialized)
                timer.cancel()

            audioBar.progress = 0
        }.onFailure { it.printStackTrace() }
    }
}