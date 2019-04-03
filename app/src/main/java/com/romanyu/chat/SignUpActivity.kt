package com.romanyu.chat

import android.app.ActionBar
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.DialogFragment
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.romanyu.chat.UserUtil.User
import com.romanyu.chat.authUtil.*
import com.romanyu.chat.dialog.VerifyEmailDialog
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity : AppCompatActivity(), View.OnClickListener, TextWatcher,
    VerifyEmailDialog.OnVerifyAndCancelListener {

    val IS_ERROR_TEXT_VIEW_VISIBLE: String = "IS_ERROR_TEXT_VIEW_VISIBLE"
    val IS_VERIFY_EMAIL_DIALOG_ERROR_TEXTVIEW_VISIBLE: String = "IS_VERIFY_EMAIL_DIALOG_ERROR_TEXTVIEW_VISIBLE"

    var verifyEmailDialog: VerifyEmailDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        sign_up_button.setOnClickListener(this)
        sign_in_button.setOnClickListener(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        username_edit_text.addTextChangedListener(this)
        email_edit_text.addTextChangedListener(this)
        password_edit_text.addTextChangedListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        val isErrorTextViewVisisble = error_text.visibility == View.VISIBLE
        outState?.putBoolean(IS_ERROR_TEXT_VIEW_VISIBLE, isErrorTextViewVisisble)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState != null) {
            val isErrorTextViewVisible = savedInstanceState.getBoolean(IS_ERROR_TEXT_VIEW_VISIBLE)
            if (isErrorTextViewVisible) {
                error_text.visibility = View.VISIBLE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        error_text.visibility = View.GONE
        progress_bar.visibility = View.GONE
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.sign_up_button -> {
                signUpButtonListener()
            }
            R.id.sign_in_button -> {
                signInButtonListener()
            }
        }
    }

    private fun signInButtonListener() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    private fun signUpButtonListener() {
        val username = username_edit_text.text.toString().toLowerCase()
        val email = email_edit_text.text.toString()
        val password = password_edit_text.text.toString()
        val isValidUsername = isValidUsername(username)
        val isValidEmail = isValidEmail(email)
        val isValidPassword = isValidPassword(password)
        if (isValidEmail && isValidPassword && isValidUsername) {
            progress_bar.visibility = View.VISIBLE
            username_input.error = null
            email_input.error = null
            password_input.error = null
            signUpWithEmailAndPassword(username, email, password)
        } else {
            if (!isValidUsername) {
                username_input.error = resources.getString(R.string.invalid_username)
            } else {
                username_input.error = null
            }
            if (!isValidEmail) {
                email_input.error = resources.getString(R.string.invalid_email)
            } else {
                email_input.error = null
            }
            if (!isValidPassword) {
                password_input.error = resources.getString(R.string.invalid_password)
            } else {
                password_input.error = null
            }
        }
    }

    fun signUpWithEmailAndPassword(username: String, email: String, password: String) {
        val mAuth = FirebaseAuth.getInstance()
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                progress_bar.visibility = View.GONE
                val user = User(
                    username = username,
                    email = email,
                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                )
                FirebaseDatabase.getInstance().reference.child("Users").child(user.userId).setValue(user.toMap())
                verifyEmailDialog = VerifyEmailDialog()
                sendEmailVerification()
            } else {
                progress_bar.visibility = View.GONE
                error_text.visibility = View.VISIBLE
            }
        }
    }

    fun sendEmailVerification() {
        val mUser = FirebaseAuth.getInstance().currentUser
        mUser?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                verifyEmailDialog?.show(supportFragmentManager, "VerifyEmail")
            } else {
                Toast.makeText(this, "Verification not sent...", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun afterTextChanged(s: Editable?) {
        sign_up_button.isEnabled = username_edit_text.text.toString().isNotEmpty() &&
                email_edit_text.text.toString().isNotEmpty() &&
                password_edit_text.text.toString().isNotEmpty()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun onCancel() {
        verifyEmailDialog?.dismiss()
        val mUser = FirebaseAuth.getInstance().currentUser
        if (mUser != null) {
            FirebaseDatabase.getInstance().reference.child("Users").child(mUser.uid).removeValue()
            mUser.delete()
        }
        signInButtonListener()
    }

    override fun onVerify() {
        verifyEmailDialog?.dismiss()
        signInCompleted(this)
    }

}
