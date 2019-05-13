package com.romanyu.chat.adapters

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import com.romanyu.chat.R
import com.romanyu.chat.SingleChatActivity
import com.romanyu.chat.UserUtil.ChatInfoBlock
import com.romanyu.chat.authUtil.FULL_DATE_PATTERN
import com.romanyu.chat.authUtil.TIME_DATE_PATTERN
import com.romanyu.chat.authUtil.getDateFromString
import com.romanyu.chat.authUtil.getDateString
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.chat_item.view.*

class ChatsAdapter(context: Context,chatsList: MutableList<ChatInfoBlock>) : RecyclerView.Adapter<ChatsAdapter.ChatView>() {

    val chatsList : MutableList<ChatInfoBlock>
    val context: Context

    init {
        this.context = context
        this.chatsList = chatsList
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ChatView {
        val view = LayoutInflater.from(context).inflate(R.layout.chat_item, viewGroup, false)
        return ChatView(view)
    }

    override fun getItemCount(): Int {
       return chatsList.size
    }

    override fun onBindViewHolder(chatView: ChatView, position: Int) {
        val chatInfoBlock = chatsList[position]
        var chatLastText = ""
        if(!chatInfoBlock.lastMessage.text.equals("")){
            chatLastText = chatInfoBlock.lastMessage.text
        }else if(!chatInfoBlock.lastMessage.imageUrl.equals("")){
            chatLastText = "Photo"
        }else if(!chatInfoBlock.lastMessage.fileUrl.equals("")){
            chatLastText = "File"
        }else{
            chatLastText = "You dont have messages history"
        }
        chatView.chatLastMessageView.text = chatLastText
        chatView.companionUsernameView.text = chatInfoBlock.companionUserName
        var timeText = ""
        if(!chatInfoBlock.lastMessage.date.equals("")){
            timeText = getDateStringForView(chatInfoBlock.lastMessage.date)
        }
        chatView.messageTimeView.text = timeText
        if(chatInfoBlock.unReadMessagesAmount > 0){
            chatView.howUnReadMessagesView.visibility = View.VISIBLE
            chatView.howUnReadMessagesView.text = chatInfoBlock.unReadMessagesAmount.toString()
        }else{
            chatView.howUnReadMessagesView.visibility = View.GONE
        }
        Glide.with(context).load(chatInfoBlock.companionProfilePhotoUrl).placeholder(R.drawable.placeholder_avatar)
            .into(chatView.companionProfilePhotoView)
        if(chatInfoBlock.companionId.equals(chatInfoBlock.lastMessage.senderUId)){
            chatView.currentUserProfilePhotoView.visibility = View.GONE
            chatView.indicatorView.visibility = View.GONE
        }else{
            chatView.currentUserProfilePhotoView.visibility = View.VISIBLE
            Glide.with(context).load(chatInfoBlock.currentUserProfilePhotoUrl).placeholder(R.drawable.placeholder_avatar)
                .into(chatView.currentUserProfilePhotoView)
            if(chatInfoBlock.lastMessage.isRead){
                chatView.indicatorView.visibility = View.GONE
            }else{
                chatView.indicatorView.visibility = View.VISIBLE
            }
        }
        chatView.itemView.setOnClickListener {
            goIntoChat(chatsList[position].companionId)
        }
        var bottomOutlineVisibility: Int
        if(position == chatsList.size - 1){
            bottomOutlineVisibility = View.GONE
        }else{
            bottomOutlineVisibility = View.VISIBLE
        }
        chatView.bottomOutline.visibility = bottomOutlineVisibility
    }

    fun goIntoChat(companionId : String){
        val intent = Intent(context, SingleChatActivity::class.java)
        intent.putExtra(SingleChatActivity.COMPANION_USER_ID, companionId)
        context.startActivity(intent)
    }

    fun getDateStringForView(dateString: String) : String{
        return getDateString(getDateFromString(dateString, FULL_DATE_PATTERN), TIME_DATE_PATTERN)
    }

    class ChatView(view: View) : RecyclerView.ViewHolder(view){
        val companionProfilePhotoView : CircleImageView;
        val companionUsernameView : TextView;
        val currentUserProfilePhotoView : CircleImageView;
        val chatLastMessageView : TextView;
        val messageTimeView : TextView
        val howUnReadMessagesView : TextView;
        val indicatorView: View
        val bottomOutline: View
        init {
            companionProfilePhotoView = view.companion_profile_photo
            companionUsernameView = view.companion_username
            currentUserProfilePhotoView = view.current_user_profile_photo
            chatLastMessageView = view.chat_last_message
            messageTimeView = view.message_time
            howUnReadMessagesView = view.how_message
            indicatorView = view.indicator
            bottomOutline = view.bottom_outline
        }
    }
}