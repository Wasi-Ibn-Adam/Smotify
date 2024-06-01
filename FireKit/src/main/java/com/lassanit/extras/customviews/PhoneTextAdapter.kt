package com.lassanit.extras.customviews

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.lassanit.extras.classes.Country
import com.lassanit.extras.classes.Designs
import com.lassanit.firekit.R

class PhoneTextAdapter(context: Context, countries: List<Country>) :
    ArrayAdapter<Country?>(
        context,
        R.layout.container_phonetext_spinner,
        R.id.phonetext_spinner_dropdown_dial_code,
        countries
    ), SpinnerAdapter {
    var design: Designs.EditText? = null
    private val mInflater: LayoutInflater = LayoutInflater.from(getContext())

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return helperView(position, convertView, parent, false)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return helperView(position, convertView, parent, true)
    }

    private fun helperView(position: Int, view: View?, parent: ViewGroup, dropdown: Boolean): View {
        return if (dropdown) {
            val holder = SpinnerHolderList(mInflater, parent, view, design)
            holder.setValue(getItem(position)!!)
            holder.v!!
        } else {
            val holder = SpinnerHolder(mInflater, parent, view, design)
            holder.setValue(getItem(position)!!)
            holder.v!!
        }
    }

    private open class SpinnerHolder(
        mInflater: LayoutInflater,
        parent: ViewGroup,
        var v: View?,
        design: Designs.EditText?
    ) {
        lateinit var mDialCode: TextView

        init {
            try {
                if (v == null) {
                    v = mInflater.inflate(R.layout.container_phonetext_spinner, parent, false)
                }
                mDialCode = v!!.findViewById(R.id.phonetext_spinner_dropdown_dial_code)
                if (design != null) {
                    mDialCode.setTextColor(ContextCompat.getColor(v!!.context, design.textColor))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun setValue(country: Country) {
            try {
                mDialCode.text = country.dialCode.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private class SpinnerHolderList(
        mInflater: LayoutInflater,
        parent: ViewGroup,
        var v: View?,
        design: Designs.EditText?
    ) {
        lateinit var mName: TextView
        lateinit var mDialCode: TextView

        init {
            try {
                if (v == null) {
                    v = mInflater.inflate(R.layout.container_phonetext_spinner_list, parent, false)
                }
                mDialCode = v!!.findViewById(R.id.phonetext_spinner_dropdown_dial_code)
                mName = v!!.findViewById(R.id.phonetext_spinner_dropdown_name)
                if (design != null) {
                    mName.setTextColor(ContextCompat.getColor(v!!.context, design.textColor))
                    mDialCode.setTextColor(ContextCompat.getColor(v!!.context, design.textColor))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun setValue(country: Country) {
            try {
                mDialCode.text = country.dialCode.toString()
                mName.text = country.getDisplayName()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
