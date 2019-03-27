package com.romanyu.chat.dialog

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
import com.google.firebase.auth.FirebaseAuth
import com.romanyu.chat.R

class ResetPasswordDialog : DialogFragment(), View.OnClickListener {

    interface OnOkListener{
        fun onOk()
    }

    private lateinit var onOkListener: OnOkListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        onOkListener = context as OnOkListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.reset_password_dialog,null)
        view.findViewById<Button>(R.id.ok_button).setOnClickListener(this)
        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        return view
    }

    override fun onClick(v: View?) {
        onOkListener.onOk()
        dialog.dismiss()
    }
}