package com.lassanit.smotify.customviews

import android.content.Context
import android.content.res.TypedArray
import android.text.InputType
import android.util.AttributeSet
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.lassanit.smotify.R

class ProfileTab(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    private var labelT: TextView
    private var valueT: TextView
    private var valueS: SwitchCompat
    private var valueEditI: ImageView

    init {
        val view = inflate(getContext(), R.layout.custom_view_profile_tab, this)
        labelT = view.findViewById(R.id.profile_tab_label)
        valueT = view.findViewById(R.id.profile_tab_value_text)
        valueS = view.findViewById(R.id.profile_tab_value_switch)
        valueEditI = view.findViewById(R.id.profile_tab_value_text_edit)
        valueEditI.visibility = GONE
        runCatching {
            val a: TypedArray =
                getContext().obtainStyledAttributes(attrs, R.styleable.ProfileTab)
            setLabel(a.getString(R.styleable.ProfileTab_label))
            setValue(a.getString(R.styleable.ProfileTab_value))
            setType(a.getInteger(R.styleable.ProfileTab_tabType, -1))
            setPassword(a.getBoolean(R.styleable.ProfileTab_isPassword, false))
            setChecked(a.getBoolean(R.styleable.ProfileTab_checked, false))
            setChecked(a.getBoolean(R.styleable.ProfileTab_checked, false))
            a.recycle()
        }.onFailure { it.printStackTrace() }
    }


    fun setPassword(value: Boolean) {
        runCatching {
            if (value)
                valueT.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
            else
                valueT.setRawInputType(InputType.TYPE_TEXT_VARIATION_NORMAL)
        }.onFailure { it.printStackTrace() }
    }

    fun setLabel(str: String?) {
        runCatching {
            labelT.visibility = when (str.isNullOrBlank()) {
                true -> {
                    GONE
                }

                false -> {
                    labelT.text = str
                    VISIBLE
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    fun setValue(str: String?) {
        runCatching {
            valueT.visibility = when (str.isNullOrBlank()) {
                true -> {
                    GONE
                }

                false -> {
                    if (str.length <= 20)
                        valueT.text = str
                    else
                        valueT.text = str.trim().dropLast(str.length - 17).plus("...")
                    VISIBLE
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    fun setType(res: Int) {
        runCatching {
            when (res) {
                0 -> {
                    valueS.visibility = VISIBLE
                }

                1 -> {
                    valueT.visibility = VISIBLE
                }

                else -> {
                    valueS.visibility = GONE
                    valueT.visibility = GONE
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    fun setChecked(value: Boolean) {
        runCatching { valueS.isChecked = value }.onFailure { it.printStackTrace() }
    }

    fun onCheckChange(onCheckedChangeListener: OnCheckedChangeListener?) {
        runCatching {
            valueS.setOnCheckedChangeListener(onCheckedChangeListener)
        }
    }

    fun onValueClickListener(onClickListener: OnClickListener?) {
        runCatching {
            valueS.setOnClickListener(onClickListener)
            valueT.setOnClickListener(onClickListener)
        }
    }

    fun onLabelClickListener(onClickListener: OnClickListener?) {
        runCatching {
            labelT.setOnClickListener(onClickListener)
        }
    }

    fun onValueEditClickListener(onClickListener: OnClickListener?) {
        runCatching {
            valueEditI.setOnClickListener(onClickListener)
        }
    }

    fun showEdit(value: Boolean) {
        runCatching {
            valueEditI.visibility = if (value) VISIBLE else GONE
        }
    }

    fun getValue(): String {
        return runCatching {
            valueT.text.toString()
        }.getOrElse { "" }
    }

}