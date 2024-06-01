package com.lassanit.extras.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import com.lassanit.extras.classes.Country
import com.lassanit.extras.classes.Designs
import com.lassanit.extras.classes.Utils
import com.lassanit.firekit.R
import java.util.Locale

class PhoneText(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    LinearLayout(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    private var designs: Designs.EditText? = null
    private var adapter: PhoneTextAdapter? = null

    var editText: EditText
    var spinner: Spinner

    private var mCountry: Country? = null

    private val mPhoneUtil = PhoneNumberUtil.getInstance()

    private var mDefaultCountryPosition = 0

    init {
        val view = inflate(getContext(), R.layout.container_phonetext, this)
        spinner = view.findViewById(R.id.fireKit_phoneText_spinner)
        editText = view.findViewById(R.id.fireKit_phoneText_editText)

        val a: TypedArray = getContext().obtainStyledAttributes(attrs, R.styleable.PhoneText)

        setBackgroundResource(
            a.getResourceId(
                R.styleable.PhoneText_android_background,
                R.drawable.firekit_shape_edittext
            )
        )

        editText.setTextAppearance(
            a.getResourceId(
                R.styleable.PhoneText_numberAppearance,
                R.style.Theme_FireKit_APP_TEXT_EDIT
            )
        )
        editText.hint = a.getString(R.styleable.PhoneText_android_hint)

        spinner.setPopupBackgroundResource(
            a.getResourceId(R.styleable.PhoneText_android_popupBackground, R.color.white)
        )

        a.recycle()
        prepareView()
    }

    private fun getUserCountryInfo(context: Context): Country {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val countryCode = telephonyManager.networkCountryIso

        val locale = Locale("", countryCode)
        val countryName = locale.displayCountry

        val countryDialCode = PhoneNumberUtil.getInstance().getCountryCodeForRegion(countryCode)

        return Country(countryCode, countryName, countryDialCode)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun prepareView() {
        adapter = PhoneTextAdapter(context, Utils.allCountries())
        val textWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                var rawNumber = s.toString()
                if (rawNumber.isEmpty()) {
                    spinner.setSelection(mDefaultCountryPosition)
                } else {
                    if (rawNumber.startsWith("00")) {
                        rawNumber = rawNumber.replaceFirst("00".toRegex(), "+")
                        editText.removeTextChangedListener(this)
                        editText.setText(rawNumber)
                        editText.addTextChangedListener(this)
                        editText.setSelection(rawNumber.length)
                    }
                    try {
                        val number = parsePhoneNumber(rawNumber)
                        if (mCountry == null || mCountry!!.dialCode != number.countryCode) {
                            selectCountry(number.countryCode)
                        }
                    } catch (ignored: NumberParseException) {
                    }
                }
            }
        }
        editText.addTextChangedListener(textWatcher)
        spinner.adapter = adapter
        spinner.setOnTouchListener { _, _ ->
            (context.getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager).hideSoftInputFromWindow(editText.windowToken, 0)
            false
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                mCountry = adapter?.getItem(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                mCountry = null
            }
        }
    }

    fun isValid(): Boolean {
        return try {
            mPhoneUtil.isValidNumber(parsePhoneNumber(getRawInput()))
        } catch (e: NumberParseException) {
            false
        }
    }

    @Throws(NumberParseException::class)
    private fun parsePhoneNumber(number: String): PhoneNumber {
        val defaultRegion = mCountry?.code?.uppercase() ?: ""
        return mPhoneUtil.parseAndKeepRawInput(number, defaultRegion)
    }

    fun getPhoneNumber(): String {
        try {
            val number = parsePhoneNumber(getRawInput())
            return mPhoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (ignored: NumberParseException) {
        }
        return getRawInput()
    }

    private fun setDefaultCountry(countryCode: String) {
        val list = Utils.allCountries()
        for (i in list.indices) {
            val country: Country = list[i]
            if (country.code.equals(countryCode, true)) {
                mCountry = country
                mDefaultCountryPosition = i
                spinner.setSelection(i)
            }
        }
    }

    fun setDefaultCountry(context: Context) {
        try {
            val obj = getUserCountryInfo(context)
            setDefaultCountry(obj.code)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectCountry(dialCode: Int) {
        val list = Utils.allCountries()
        for (i in list.indices) {
            val country: Country = list[i]
            if (country.dialCode == dialCode) {
                mCountry = country
                spinner.setSelection(i)
            }
        }
    }

    fun setPhoneNumber(rawNumber: String) {
        try {
            val number: PhoneNumber = parsePhoneNumber(rawNumber)
            if (mCountry == null || mCountry!!.dialCode != number.countryCode) {
                selectCountry(number.countryCode)
            }
            editText.setText(mPhoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL))
        } catch (ignored: NumberParseException) {
        }
    }

    fun setHint(resId: Int) {
        editText.setHint(resId)
    }

    fun setHint(resId: String) {
        editText.hint = resId
    }

    fun setError(error: String?) {
        editText.error = error
    }

    fun setDesign(designs: Designs.EditText?) {
        this.designs = designs
        if (designs == null)
            return
        setHintColor(designs.hintColor)
        setTextColor(designs.textColor)
        setBackgroundResource(0)
        setBackgroundResource(designs.background)
        adapter?.design = designs
        spinner.setPopupBackgroundResource(designs.background)
    }

    private fun setTextColor(resId: Int) {
        editText.setTextColor(resId)
    }

    private fun setHintColor(resId: Int) {
        editText.setHintTextColor(resId)
    }

    private fun getRawInput(): String {
        return editText.text.toString()
    }

}