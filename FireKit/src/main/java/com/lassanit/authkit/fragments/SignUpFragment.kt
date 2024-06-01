package com.lassanit.authkit.fragments

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.lassanit.extras.classes.Utils
import com.lassanit.firekit.R
import com.lassanit.kit.FireKitCompatActivity.Companion.options
import java.util.Objects
import kotlin.collections.set

class SignUpFragment :
    AuthFragment(R.layout.fragment_auth_signup) {

    private lateinit var txtEmail: EditText
    private lateinit var txtPass: EditText
    private lateinit var txtConPass: EditText

    private lateinit var txtEmailL: TextInputLayout
    private lateinit var txtPassL: TextInputLayout
    private lateinit var txtConPassL: TextInputLayout


    private lateinit var btn: Button
    private var e = ""
    private var p = ""
    private var cp = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actions.onFragmentLoaded(SignUpFragment::class.java.simpleName, view)
        txtEmail = view.findViewById(R.id.fireKit_editText_email)
        txtPass = view.findViewById(R.id.fireKit_editText_password)
        txtConPass = view.findViewById(R.id.fireKit_editText_passwordConfirm)

        txtEmailL = view.findViewById(R.id.lay_email)
        txtPassL = view.findViewById(R.id.lay_pass)
        txtConPassL = view.findViewById(R.id.lay_pass_con)

        btn = view.findViewById(R.id.fireKit_button_default)

        btn.setOnClickListener {
            Utils.hideSoftKeyboard(requireActivity())
            val email = txtEmail.text.toString()
            val pass = txtPass.text.toString()
            val cpass = txtConPass.text.toString()
            if (email.isEmpty()) {
                txtEmailL.error = "Missing"
                return@setOnClickListener
            } else if (!email.matches(Patterns.EMAIL_ADDRESS.toRegex())) {
                txtEmailL.error = "Invalid"
                return@setOnClickListener
            }
            if (pass.isEmpty()) {
                txtPassL.error = "Missing"
                return@setOnClickListener
            } else if (pass.length < 8) {
                txtPassL.error = "Invalid"
                return@setOnClickListener
            }
            if (cpass.isEmpty()) {
                txtConPassL.error = "Missing"
                return@setOnClickListener
            } else if (cpass != pass) {
                txtConPassL.error = "Not same as Above"
                return@setOnClickListener
            }
            action(email, pass)
        }
        handleError()
    }


    private fun handleError() {
        runCatching {
            handleError(txtEmailL,txtEmail)
            handleError(txtPassL,txtPass)
            handleError(txtConPassL,txtConPass)
        }
    }

    override fun onPause() {
        super.onPause()
        e = txtEmail.text.toString()
        p = txtPass.text.toString()
        cp = txtConPass.text.toString()
    }

    override fun onResume() {
        super.onResume()
        txtEmail.setText(e)
        txtPass.setText(p)
        txtConPass.setText(cp)
    }

    private fun action(email: String, pass: String) {
        actions.loadingPopup(true)
        options.getAuth().createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                actions.log("createUserWithEmailAndPassword: SUCCESS.")
                if (task.isSuccessful) {
                    val user: FirebaseUser? = task.result.user
                    if (user != null) {
                        if (task.result.additionalUserInfo != null)
                            actions.onAdditionalInformation(task.result.additionalUserInfo!!)
                        actions.onSignUpComplete(Utils.SignInMethod.EMAIL)
                    }
                }
                actions.loadingPopup(false)
            }
            .addOnFailureListener { e ->
                try {
                    throw Objects.requireNonNull(e)
                } catch (weakPassword: FirebaseAuthWeakPasswordException) {
                    actions.log(
                        "createUserWithEmailAndPassword: ERROR: FirebaseAuthWeakPasswordException"
                    )
                    txtPassL.error = "Weak Password."
                } catch (malformedEmail: FirebaseAuthInvalidCredentialsException) {
                    actions.log(
                        "createUserWithEmailAndPassword: ERROR: FirebaseAuthInvalidCredentialsException"
                    )
                    txtEmailL.error = "Mail-formed Email."
                } catch (existEmail: FirebaseAuthUserCollisionException) {
                    actions.log(
                        "createUserWithEmailAndPassword: ERROR: FirebaseAuthUserCollisionException"
                    )
                    txtEmailL.error = "Email already Exist."
                } catch (e1: Exception) {
                    txtEmailL.error = "You Cant Register with this Email."
                    actions.log("createUserWithEmailAndPassword: ERROR: " + e1.message)
                }
            }
    }

    override fun getDefaultLinker(): HashMap<Int, View> {
        val map: HashMap<Int, View> = super.getDefaultLinker()
        try {
            map[R.string.tag_edittext_default] = txtEmail
            map[R.string.tag_edittext_extra] = txtPass
            map[R.string.tag_button_default] = btn
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

}