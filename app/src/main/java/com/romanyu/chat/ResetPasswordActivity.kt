package com.romanyu.chat

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.DialogFragment
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.romanyu.chat.authUtil.isValidEmail
import com.romanyu.chat.dialog.ResetPasswordDialog
import kotlinx.android.synthetic.main.activity_reset_password.*

class ResetPasswordActivity : AppCompatActivity(), View.OnClickListener, TextWatcher, ResetPasswordDialog.OnOkListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        val toolbar:Toolbar? = findViewById(R.id.toolbar) as? Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        reset_button.setOnClickListener(this)
        sign_up_button.setOnClickListener(this)
        email_edit_text.addTextChangedListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.reset_button ->{
                resetButtonListener()
            }
            R.id.sign_up_button ->{
                val intent = Intent(this,SignUpActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
        }
    }

    private fun resetButtonListener(){
        val email = email_edit_text.text.toString()
        val isValidEmail = isValidEmail(email)
        if(isValidEmail){
            email_input.error = null
            resetPassword(email)

        }else{
            email_input.error = resources.getString(R.string.invalid_email)
        }
    }

    private fun resetPassword(email:String){
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener {
            task -> if(task.isSuccessful){
                val resetPasswordDialog:DialogFragment = ResetPasswordDialog()
                resetPasswordDialog.show(supportFragmentManager,"ResetPassword")
            }else{
                Toast.makeText(this,"Message not sent in your mail, try again...",Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onOk() {
        val intent = Intent(this,SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    override fun afterTextChanged(s: Editable?) {
        reset_button.isEnabled = email_edit_text.text.toString().isNotEmpty()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}
