package com.lassanit.smotify.customviews

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.SwitchCompat
import com.bumptech.glide.Glide
import com.lassanit.smotify.R

class SettingsTab(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    private var titleT: TextView
    private var descT: TextView
    private var logoI: ImageView
    private var imgI: ImageView
    private var switchS: SwitchCompat
    private var seekLay: LinearLayout
    private var seekMin: TextView
    private var seekMax: TextView
    private var seekBar: AppCompatSeekBar
    private var gif: Int = -1
    private var img: Int = -1
    private var tint: Int = -1

    init {
        val view = inflate(getContext(), R.layout.custom_view_settings_tab, this)
        titleT = view.findViewById(R.id.setting_tab_title)
        descT = view.findViewById(R.id.setting_tab_desc)
        logoI = view.findViewById(R.id.setting_tab_logo)
        imgI = view.findViewById(R.id.setting_tab_img)
        seekBar = view.findViewById(R.id.setting_tab_seekbar)
        seekLay = view.findViewById(R.id.setting_tab_seekbar_lay)
        seekMax = view.findViewById(R.id.setting_tab_seekbar_max)
        seekMin = view.findViewById(R.id.setting_tab_seekbar_min)
        switchS = view.findViewById(R.id.setting_tab_switch)
        runCatching {
            val a: TypedArray =
                getContext().obtainStyledAttributes(attrs, R.styleable.SettingsTab)
            setTitle(a.getString(R.styleable.SettingsTab_android_title))
            setDesc(a.getString(R.styleable.SettingsTab_desc))
            setGif(a.getResourceId(R.styleable.SettingsTab_gif, -1))
            setImage(a.getResourceId(R.styleable.SettingsTab_android_src, R.drawable.person))
            setImageTint(a.getColor(R.styleable.SettingsTab_logoTint, -1))
            setEndView(a.getInt(R.styleable.SettingsTab_extraType, -1))
            setSeekbar(a.getBoolean(R.styleable.SettingsTab_seekbar, false))
            progress(a.getInt(R.styleable.SettingsTab_progress, 1))
            progressLimits(
                a.getInt(R.styleable.SettingsTab_max, 10),
                a.getInt(R.styleable.SettingsTab_min, 1)
            )
            setEndIcon(
                a.getResourceId(
                    R.styleable.SettingsTab_endIcon,
                    com.lassanit.firekit.R.drawable.base_forward
                )
            )
            a.recycle()
        }
    }

    fun setImageTint(res: Int) {
        runCatching {
            if (res == -1) {
                logoI.imageTintList = null
                return@runCatching
            }
            tint = res
            logoI.imageTintList = ColorStateList.valueOf(res)
        }
    }

    fun setGif(@DrawableRes res: Int) {
        runCatching {
            if (res == -1) return
            gif = res
            Glide.with(this).asGif().load(res).into(logoI)
        }
    }

    fun setImage(@DrawableRes res: Int) {
        runCatching {
            img = res
            logoI.setImageResource(res)
        }
    }

    fun setTitle(str: String?) {
        runCatching {
            titleT.visibility = when (str.isNullOrBlank()) {
                true -> {
                    GONE
                }

                false -> {
                    titleT.text = str
                    VISIBLE
                }
            }
        }
    }

    fun getDesc(): String? {
        return runCatching { descT.text.toString() }.getOrNull()
    }

    fun setDesc(str: String?) {
        runCatching {
            descT.visibility = when (str.isNullOrBlank()) {
                true -> {
                    GONE
                }

                false -> {
                    descT.text = str
                    VISIBLE
                }
            }
        }
    }

    fun setSeekbar(show: Boolean) {
        runCatching {
            seekLay.visibility = if (show) VISIBLE else GONE
        }
    }

    fun progress(value: Int) {
        runCatching {
            seekBar.progress = value
        }
    }

    fun progressLimits(max: Int, min: Int) {
        runCatching {
            seekBar.max = max
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                seekBar.min = min
            }

            seekMin.text = min.toString()
            seekMax.text = max.toString()
        }
    }

    private fun setEndIcon(@DrawableRes res: Int) {
        runCatching {
            imgI.setImageResource(res)
        }
    }

    private fun setEndView(v: Int) {
        runCatching {
            when (v) {
                0 -> {
                    imgI.visibility = VISIBLE
                }

                1 -> {
                    switchS.visibility = VISIBLE
                }
            }
        }
    }

    fun setCheck(value: Boolean) {
        runCatching { switchS.isChecked = value }
    }

    fun onCheckChange(onCheckedChangeListener: OnCheckedChangeListener?) {
        runCatching {
            switchS.setOnCheckedChangeListener(onCheckedChangeListener)
        }
    }

    fun onCheckClickListener(onClickListener: OnClickListener?) {
        runCatching {
            switchS.setOnClickListener(onClickListener)
        }
    }

    fun onProgressListener(onSeek: (Int) -> Unit) {
        runCatching {
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    onSeek(p1)
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}

                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
    }

    fun onTextClickListener(onClickListener: OnClickListener?) {
        runCatching {
            titleT.setOnClickListener(onClickListener)
            descT.setOnClickListener(onClickListener)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        runCatching {
            switchS.isEnabled = enabled
        }
        runCatching {
            if (enabled) {
                titleT.alpha = 1.0F
                descT.alpha = 1.0F
                if (gif != -1 && img != -1) {
                    setGif(gif)
                }
                setImageTint(tint)
            } else {
                if (gif != -1 && img != -1) {
                    setImage(img)
                }
                logoI.imageTintList = ColorStateList.valueOf(Color.GRAY)
                titleT.alpha = 0.5F
                descT.alpha = 0.5F
            }
        }
    }
}