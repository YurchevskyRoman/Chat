package com.romanyu.chat

import android.content.Context
import com.romanyu.chat.authUtil.*
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.romanyu.chat.dialog.VerifyEmailDialog
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity(), View.OnClickListener,TextWatcher {

    val IS_ERROR_TEXT_VIEW_VISIBLE:String = "IS_ERROR_TEXT_VIEW_VISIBLE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        val user:FirebaseUser? = FirebaseAuth.getInstance().currentUser
        if(user != null){
            if(isEmailVerify()){
                signInCompleted(this)
            }else{
                FirebaseDatabase.getInstance().reference.child("Users").child(user.uid).removeValue()
                user.delete()
            }
        }
        val toolbar:Toolbar? = findViewById(R.id.toolbar) as? Toolbar
        setSupportActionBar(toolbar)
        sign_in_button.setOnClickListener(this)
        sign_up_button.setOnClickListener(this)
        forgot_password_button.setOnClickListener(this)
        email_edit_text.addTextChangedListener(this)
        password_edit_text.addTextChangedListener(this)
    }

    override fun onStart() {
        super.onStart()
        error_text.visibility = View.GONE
        progress_bar.visibility = View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        val isErrorTextViewVisisble = error_text.visibility == View.VISIBLE
        outState?.putBoolean(IS_ERROR_TEXT_VIEW_VISIBLE,isErrorTextViewVisisble)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if(savedInstanceState != null) {
            val isErrorTextViewVisible = savedInstanceState.getBoolean(IS_ERROR_TEXT_VIEW_VISIBLE)
            if(isErrorTextViewVisible){
                error_text.visibility = View.VISIBLE
            }
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.sign_in_button ->{
                signInButtonListener()
            }
            R.id.sign_up_button ->{
                val intent = Intent(this,SignUpActivity::class.java)
                startActivity(intent)
            }
            R.id.forgot_password_button ->{
                val intent = Intent(this,ResetPasswordActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun signInButtonListener(){
        progress_bar.visibility = View.VISIBLE
        val email = email_edit_text.text.toString()
        val password = password_edit_text.text.toString()
        val isValidEmail = isValidEmail(email)
        val isValidPassword = isValidPassword(password)
        if(isValidEmail && isValidPassword){
            email_input.error = null
            password_input.error = null
            signInWithEmailAndPassword(email,password)
        }else{
            if(!isValidEmail){
                email_input.error = resources.getString(R.string.invalid_email)
            }else{
                email_input.error = null
            }
            if(!isValidPassword){
                password_input.error = resources.getString(R.string.invalid_password)
            }else{
                password_input.error = null
            }
        }
    }

    private fun signInWithEmailAndPassword(email:String,password:String) {
        val mAuth = FirebaseAuth.getInstance()
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener{
            task -> if(task.isSuccessful){
                progress_bar.visibility = View.GONE
                signInCompleted(this)
            }else{
                progress_bar.visibility = View.GONE
                error_text.visibility = View.VISIBLE
            }
        }
    }

    override fun afterTextChanged(s: Editable?) {
          sign_in_button.isEnabled = email_edit_text.text.toString().isNotEmpty() && password_edit_text.text.toString().isNotEmpty()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}
