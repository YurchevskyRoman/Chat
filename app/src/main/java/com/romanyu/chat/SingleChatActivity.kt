package com.romanyu.chat

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.romanyu.chat.UserUtil.User
import kotlinx.android.synthetic.main.activity_single_chat.*
import kotlin.math.log

class SingleChatActivity : AppCompatActivity(), TextWatcher {

    companion object {
        val COMPANION_USER_ID = "COMPANION_USER_ID"
    }

    var companion: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_chat)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val companionId = intent.extras.getString(COMPANION_USER_ID)
        if (companionId != null) {
            readCompanionData(companionId)
        }
        message_text.addTextChangedListener(this)
    }

    override fun afterTextChanged(s: Editable?) {

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if(s?.length ?: 0 > 0){
            attach_file_button.visibility = View.GONE
            send_button.visibility = View.VISIBLE
        }else{
            attach_file_button.visibility = View.VISIBLE
            send_button.visibility = View.GONE
        }
    }

    fun readCompanionData(companionId: String) {
        FirebaseDatabase.getInstance().reference.child("Users").child(companionId)
            .child("UserInfo").addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        companion = dataSnapshot.getValue(User::class.java)
                        username.text = companion?.username
                        email.text = companion?.email
                        Glide.with(this@SingleChatActivity).load(companion?.imageUrl)
                            .placeholder(R.drawable.placeholder_avatar)
                            .into(profile_photo)
                    }

                    override fun onCancelled(dataSnapshot: DatabaseError) {

                    }
                }
            )
    }
}
