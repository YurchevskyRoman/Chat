package com.romanyu.chat.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.romanyu.chat.R
import com.romanyu.chat.authUtil.isEmailVerify
import kotlinx.android.synthetic.main.activity_sign_in.*

class VerifyEmailDialog : DialogFragment(),View.OnClickListener {

    interface OnVerifyAndCancelListener{
        fun onCancel()
        fun onVerify()
    }

    private lateinit var verifyAndCancelListener: OnVerifyAndCancelListener
    private lateinit var errorTextView:TextView

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        verifyAndCancelListener = context as OnVerifyAndCancelListener
    }

    override fun onStart() {
        super.onStart()
        progress_bar.visibility = View.GONE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view:View = inflater.inflate(R.layout.verify_email_dialog,null)
        errorTextView = view.findViewById(R.id.verify_error_text)
        view.findViewById<Button>(R.id.verify_button).setOnClickListener(this)
        view.findViewById<Button>(R.id.cancel_button).setOnClickListener(this)
        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        return view
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.verify_button ->{
                    reloadAndVerifyEmail()
            }
            R.id.cancel_button ->{
                    verifyAndCancelListener.onCancel()
            }
        }
    }

    fun reloadAndVerifyEmail(){
        progress_bar.visibility = View.VISIBLE
        val mUser = FirebaseAuth.getInstance().currentUser
        mUser?.reload()?.addOnSuccessListener(
            object : OnSuccessListener<Void>{
                override fun onSuccess(mVoid: Void?) {
                    progress_bar.visibility = View.GONE
                    if(isEmailVerify()  ){
                        verifyAndCancelListener.onVerify()
                    }else{
                        errorTextView.visibility = View.VISIBLE
                    }
                }
            }
        )
    }
}