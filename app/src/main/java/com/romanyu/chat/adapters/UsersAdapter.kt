package com.romanyu.chat.adapters

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.romanyu.chat.R
import com.romanyu.chat.SingleChatActivity
import com.romanyu.chat.UserUtil.User
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.profile_item.view.*

class UsersAdapter(
    val context: Context,
    val usersList: MutableList<User>
) : RecyclerView.Adapter<UsersAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.profile_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, index: Int) {
        val user = usersList[index]
        viewHolder.usernameTextView.text = user.username
        viewHolder.emailTextView.text = user.email
        Glide.with(context).load(user.imageUrl).placeholder(R.drawable.placeholder_avatar)
            .into(viewHolder.profileImageView)
        viewHolder.itemView.setOnClickListener {
            val intent = Intent(context,SingleChatActivity::class.java)
            val bundle = Bundle()
            bundle.putString(SingleChatActivity.COMPANION_USER_ID,user.userId)
            intent.putExtras(bundle)
            context.startActivity(intent)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImageView: CircleImageView
        val usernameTextView: TextView
        val emailTextView: TextView

        init {
            profileImageView = view.profile_photo
            usernameTextView = view.username
            emailTextView = view.email
        }
    }
}