package com.romanyu.chat.adapters

import android.content.Context
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.romanyu.chat.R
import com.romanyu.chat.UserUtil.Message
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.own_message_layout.view.*

class MessagesAdapter(
    val context: Context,
    val messagesList: MutableList<Message>,
    val checkedMessagesList: MutableList<Message>,
    val checkedOrNotMessagesList: MutableList<Boolean>
) : RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {

    val currentUserId: String
    val checkableMessages: ICheckableMessages

    init {
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        checkableMessages = context as ICheckableMessages
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(viewType, viewGroup, false)
        return ViewHolder(view,viewType)
    }

    override fun getItemCount(): Int {
        return messagesList.size
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, index: Int) {
        val message = messagesList[index]
        viewHolder.messageTextView.text = message.text
        Log.d("ryu",message.isRead.toString())
        if(isOwnMessage(index) && !message.isRead){
            viewHolder.indicator?.visibility = View.VISIBLE
        }
        viewHolder.itemView.setOnLongClickListener(
            object : View.OnLongClickListener {
                override fun onLongClick(v: View?): Boolean {
                    onCheckMessage(viewHolder, index)
                    return true
                }
            }
        )
        viewHolder.itemView.setOnClickListener {
            if (checkedMessagesList.isNotEmpty()) {
                onCheckMessage(viewHolder, index)
            }
        }
    }

    fun onCheckMessage(viewHolder: ViewHolder, index: Int) {
        if(!isOwnMessage(index)){
            return
        }
        if (checkedOrNotMessagesList[index]) {
            checkedOrNotMessagesList[index] = false
            viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT)
            checkedMessagesList.remove(messagesList[index])
        } else {
            checkedOrNotMessagesList[index] = true
            viewHolder.itemView.setBackgroundColor(context.resources.getColor(R.color.checked_message_background))
            checkedMessagesList.add(messagesList[index])
        }
        checkableMessages.onCheckMessage()
    }

    fun isOwnMessage(index: Int) : Boolean{
        return messagesList[index].senderUId == currentUserId
    }

    override fun getItemViewType(position: Int): Int {
        if (isOwnMessage(position)) {
            return R.layout.own_message_layout
        } else {
            return R.layout.companion_message_layout
        }
    }

    class ViewHolder(view: View, viewType: Int) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView
        val indicator: CircleImageView?
        init {
            messageTextView = view.message_text
            if(viewType == R.layout.own_message_layout){
                indicator = view.indicator
            }else{
                indicator = null
            }
        }
    }


    interface ICheckableMessages {
        fun onCheckMessage()
    }
}