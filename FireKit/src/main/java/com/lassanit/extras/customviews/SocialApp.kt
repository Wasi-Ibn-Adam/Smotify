package com.lassanit.extras.customviews

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.lassanit.firekit.R

class SocialApp(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    LinearLayout(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    val img: ImageView
    val name: TextView

    init {
        val view = inflate(getContext(), R.layout.container_social_app, this)
        img = view.findViewById(R.id.social_app_img)
        name = view.findViewById(R.id.social_app_name)

        val a: TypedArray = getContext().obtainStyledAttributes(attrs, R.styleable.SocialApp)

        setText(a.getString(R.styleable.SocialApp_appName))
        setImageResource(a.getResourceId(R.styleable.SocialApp_img, R.drawable.app_logo))
        a.recycle()
    }

    private fun titleCase(input: String?): String {
        if (input.isNullOrEmpty()) return "" // Handle empty string
        return input.substring(0, 1).uppercase() + input.substring(1).lowercase()
    }

    fun setText(str: String?) {
        runCatching {
            name.visibility = if (str.isNullOrBlank()) {
                GONE
            } else {
                name.text = titleCase(str)
                VISIBLE
            }
        }
    }

    fun setImageResource(@DrawableRes res: Int) {
        runCatching { img.setImageResource(res) }
    }

    fun setMinimal() {
        runCatching {
            background = null
            bottom=0
            top=0
            left=0
            right=0
        }.onFailure { it.printStackTrace() }
    }
}