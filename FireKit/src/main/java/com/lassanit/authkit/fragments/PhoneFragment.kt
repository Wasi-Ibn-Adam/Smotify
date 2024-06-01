package com.lassanit.authkit.fragments

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat.setDecorFitsSystemWindows
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.lassanit.extras.classes.Utils
import com.lassanit.extras.customviews.PhoneText
import com.lassanit.firekit.R
import com.lassanit.kit.FireKitCompatActivity.Companion.options
import com.raycoarana.codeinputview.CodeInputView
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit


class PhoneFragment : AuthFragment(R.layout.fragment_auth_phone) {

    private lateinit var txtPhone: PhoneText
    private lateinit var txtTimer: TextView
    private lateinit var txtResend: TextView
    private lateinit var txtCode: CodeInputView
    private lateinit var btnPhone: Button

    private var time = 60L
    private var timer: Timer? = null
    private var task: TimerTask? = null
    private var resendToken: ForceResendingToken? = null
    private var phoneNumber: String? = null
    private var verificationId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleSize(true)

        actions.onFragmentLoaded(PhoneFragment::class.java.simpleName, view)
        txtPhone = view.findViewById(R.id.fireKit_phoneText)
        btnPhone = view.findViewById(R.id.fireKit_button_default)
        txtCode = view.findViewById(R.id.fireKit_codeInput)
        txtResend = view.findViewById(R.id.txt_resend)
        txtTimer = view.findViewById(R.id.txt_timer)

        txtCode.visibility = View.GONE
        txtResend.visibility = View.GONE
        txtTimer.visibility = View.GONE
        btnPhone.setOnClickListener {
            if (!txtPhone.isValid()) {
                txtPhone.editText.error = "Enter Valid Phone Number"
                return@setOnClickListener
            }
            val num: String = txtPhone.getPhoneNumber()
            action(num)
            Utils.hideSoftKeyboard(requireActivity())
        }
        txtCode.addOnCompleteListener { code: String ->
            onPhoneCodeEntered(code)
            txtTimer.visibility = View.GONE
            txtResend.visibility = View.GONE
        }
        txtResend.setOnClickListener { onPhoneCodeResent() }

        txtPhone.setDefaultCountry(requireContext())
    }

    private fun action(num: String) {
        actions.loadingPopup(true)
        actions.backPress(false)
        phoneNumber = num
        timer = Timer()
        task = object : TimerTask() {
            @SuppressLint("SetTextI18n")
            override fun run() {
                try {
                    if (time < 0)
                        return
                    requireActivity().runOnUiThread { txtTimer.text = "$time sec" }
                    time--
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val options = PhoneAuthOptions.newBuilder(options.getAuth())
            .setPhoneNumber(num) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(requireActivity()) // (optional) Activity for callback binding
            // If no activity is passed, reCAPTCHA verification can not be used.
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun onPhoneCodeResent() {
        actions.loadingPopup(true)
        val options = PhoneAuthOptions.newBuilder(options.getAuth())
            .setForceResendingToken(resendToken!!)
            .setPhoneNumber(phoneNumber!!) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(requireActivity()) // (optional) Activity for callback binding
            // If no activity is passed, reCAPTCHA verification can not be used.
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun onPhoneCodeEntered(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private var callbacks: OnVerificationStateChangedCallbacks =
        object : OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                actions.log("Phone onVerificationCompleted.")
                txtCode.code = phoneAuthCredential.smsCode
                signInWithPhoneAuthCredential(phoneAuthCredential)
                actions.backPress(true)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                actions.loadingPopup(false)
                actions.backPress(true)
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        actions.log("Phone onVerificationFailed: FirebaseAuthInvalidCredentialsException.")
                        // Invalid request
                        txtPhone.editText.error = "Invalid Phone number."
                    }

                    is FirebaseTooManyRequestsException -> {
                        actions.log("Phone onVerificationFailed: FirebaseTooManyRequestsException.")
                        // The SMS quota for the project has been exceeded
                        txtPhone.editText.error =
                            "We have blocked all requests from this device due to unusual activity."
                    }

                    is FirebaseAuthMissingActivityForRecaptchaException -> {
                        // reCAPTCHA verification attempted with null Activity
                        actions.log(
                            "Phone onVerificationFailed: FirebaseAuthMissingActivityForRecaptchaException."
                        )
                        txtPhone.editText.error = "Try again later."
                    }

                    is FirebaseAuthInvalidUserException -> {
                        actions.log(
                            "Phone onVerificationFailed: FirebaseAuthInvalidUserException."
                        )
                        txtPhone.editText.error = "Try again later."
                    }

                    is FirebaseAuthException -> {
                        actions.log(
                            "Phone onVerificationFailed: FirebaseAuthException."
                        )
                        txtPhone.editText.error = "Try again later."

                    }

                    is FirebaseNetworkException -> {
                        actions.log(
                            "Phone onVerificationFailed: FirebaseNetworkException."
                        )
                        Toast.makeText(requireContext(),"Check your Internet-Connection and Try again.",Toast.LENGTH_SHORT).show()
                    }

                    else -> {
                        e.printStackTrace()
                        actions.log("Phone onVerificationFailed: ERROR: " + e.message)
                        txtPhone.editText.error = "Try again later."
                    }
                }
            }

            override fun onCodeSent(
                verifyId: String,
                token: ForceResendingToken
            ) {
                actions.log("Phone onCodeSent.")
                Toast.makeText(context, "", Toast.LENGTH_SHORT).show()
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                actions.loadingPopup(false)
                // by combining the code with a verification ID.
                // Save verification ID and resending token so we can use them later
                verificationId = verifyId
                resendToken = token
                txtPhone.visibility = View.GONE
                btnPhone.visibility = View.GONE
                txtCode.visibility = View.VISIBLE
                txtTimer.visibility = View.VISIBLE
                txtCode.requestFocus()
                txtCode.setEditable(true)
                try {
                    timer = Timer()
                    timer!!.schedule(task, 0, 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCodeAutoRetrievalTimeOut(s: String) {
                super.onCodeAutoRetrievalTimeOut(s)
                actions.log("Phone onCodeAutoRetrievalTimeOut.")
                txtTimer.visibility = View.GONE
                txtResend.visibility = View.VISIBLE
                actions.backPress(true)
            }
        }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        options.getAuth().signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                actions.log("Phone signInWithCredential: SUCCESS.")
                timer!!.cancel()
                val user: FirebaseUser? = authResult.user
                if (user != null) {
                    actions.log("Phone signInWithCredential: SUCCESS USER.")
                    authResult.additionalUserInfo?.let { actions.onAdditionalInformation(it) }
                    if (authResult.additionalUserInfo?.isNewUser == true) {
                        actions.onSignUpComplete(Utils.SignInMethod.PHONE)
                    } else actions.onSignInComplete()
                } else actions.log("Phone signInWithCredential: SUCCESS NULL.")

            }
            .addOnFailureListener { e ->
                if (e is FirebaseAuthInvalidCredentialsException) {
                    // The verification code entered was invalid
                    actions.log(
                        "Phone signInWithCredential: FirebaseAuthInvalidCredentialsException."
                    )
                    txtCode.error = "Invalid Code"
                    txtCode.setEditable(true)
                    txtCode.visibility = View.VISIBLE
                    txtTimer.visibility = View.VISIBLE
                } else {
                    actions.log("Phone signInWithCredential: ERROR.")
                }
            }
    }

    override fun getDefaultLinker(): HashMap<Int, View> {
        val map: HashMap<Int, View> = super.getDefaultLinker()
        try {
            map[R.string.tag_button_extra] = btnPhone
            map[R.string.tag_edittext_default] = txtPhone
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    override fun onDestroy() {
        super.onDestroy()
        handleSize(false)
    }

    private fun handleSize(enter: Boolean) {
        try {
            val act = requireActivity()
            setDecorFitsSystemWindows(act.window, enter.not())
            if (enter) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val rootView = act.findViewById<View>(R.id.top_frame)
                    rootView.setOnApplyWindowInsetsListener { _, windowInsets ->
                        val imeHeight = windowInsets.getInsets(WindowInsets.Type.ime()).bottom
                        rootView.setPadding(0, 0, 0, imeHeight)
                        val insets =
                            windowInsets.getInsets(WindowInsets.Type.ime() or WindowInsets.Type.systemGestures())
                        windowInsets.inset(insets)
                    }
                } else
                    act.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            } else {
                act.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}