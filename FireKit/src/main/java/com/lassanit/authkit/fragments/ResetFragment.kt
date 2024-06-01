package com.lassanit.authkit.fragments

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.lassanit.extras.classes.Utils
import com.lassanit.firekit.R
import com.lassanit.kit.FireKitCompatActivity.Companion.options

class ResetFragment : AuthFragment(R.layout.fragment_auth_reset) {

    private lateinit var txtEmail: EditText
    private lateinit var txtEmailL: TextInputLayout
    private lateinit var btn: Button
    private var e = ""
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actions.onFragmentLoaded(ResetFragment::class.java.simpleName, view)
        txtEmail = view.findViewById(R.id.fireKit_editText_email)
        txtEmailL = view.findViewById(R.id.lay_email)
        btn = view.findViewById(R.id.fireKit_button_default)
        btn.setOnClickListener {
            Utils.hideSoftKeyboard(requireActivity())
            val email: String = txtEmail.text.toString()
            if (email.isEmpty()) {
                txtEmailL.error = "Missing"
                return@setOnClickListener
            } else if (!email.matches(Patterns.EMAIL_ADDRESS.toRegex())) {
                txtEmailL.error = "Invalid"
                return@setOnClickListener
            }
            action(email)
        }
        btn.setOnLongClickListener {
            Toast.makeText(
                requireContext(),
                "Send Reset-Password Link",
                Toast.LENGTH_SHORT
            ).show();true
        }

        handleError(txtEmailL,txtEmail)
    }

    override fun onPause() {
        super.onPause()
        e = txtEmail.text.toString()
    }

    override fun onResume() {
        super.onResume()
        txtEmail.setText(e)
    }

    private fun action(email: String) {
        actions.loadingPopup(true)
        options.getAuth().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    actions.log("sendPasswordResetEmail: SUCCESS.")
                    actions.onResetLinkSent()
                }
                actions.loadingPopup(false)
            }
            .addOnFailureListener { e ->
                actions.log("sendPasswordResetEmail: ERROR: " + e.message)
                if (e.localizedMessage != null && e.localizedMessage
                        .equals(
                            "There is no user record corresponding to this identifier. The user may have been deleted.",
                            true
                        )
                ) txtEmailL.error = "Email not registered."
                e.printStackTrace()
            }
    }

    override fun getDefaultLinker(): HashMap<Int, View> {
        val map: HashMap<Int, View> = super.getDefaultLinker()
        map[R.string.tag_edittext_default] = txtEmail
        map[R.string.tag_button_default] = btn
        return map
    }

}