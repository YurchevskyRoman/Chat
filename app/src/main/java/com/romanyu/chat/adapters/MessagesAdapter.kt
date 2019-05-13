package com.romanyu.chat.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.romanyu.chat.ImageActivity
import com.romanyu.chat.R
import com.romanyu.chat.SingleChatActivity
import com.romanyu.chat.UserUtil.Message
import com.romanyu.chat.authUtil.FULL_DATE_PATTERN
import com.romanyu.chat.authUtil.TIME_DATE_PATTERN
import com.romanyu.chat.authUtil.getDateFromString
import com.romanyu.chat.authUtil.getDateString
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.own_message_layout.view.*

class MessagesAdapter(
    val context: Context,
    val messagesList: MutableList<Message>,
    val checkedMessagesList: MutableList<Message>,
    val checkedOrNotMessagesList: MutableList<Boolean>,
    val userProfilePhotos: HashMap<String, Any>
) : RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {

    val currentUserId: String
    val checkableMessages: ICheckableMessages
    val deleteMessagesPositions = ArrayList<Int>()

    init {
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid as String
        checkableMessages = context as ICheckableMessages

        userProfilePhotos.forEach {
            Log.d("ryu", "key - " + it.key + " value - " + it.value)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(viewType, viewGroup, false)
        return ViewHolder(view, viewType)
    }

    override fun getItemCount(): Int {
        return messagesList.size
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, index: Int) {
        val message = messagesList[index]

        if(!message.imageUrl.equals("")){
           viewHolder.setMessageImage(message.imageUrl)
        }else if(!message.text.equals("")){
            viewHolder.setMessageText(message.text)
        }else{

        }
        val date = getDateStringForView(message.date)
        viewHolder.dateTextView.text = date

        val photoUrl = userProfilePhotos[message.senderUId]

        Glide.with(context).load(photoUrl)
            .placeholder(R.drawable.placeholder_avatar)
            .into(viewHolder.profilePhotoView)

        if (isOwnMessage(index) && !message.isRead) {
            viewHolder.indicator?.visibility = View.VISIBLE
        } else {
            viewHolder.indicator?.visibility = View.GONE
        }
        if (checkedOrNotMessagesList[index]) {
            setAsCheckMessage(viewHolder, index)
        } else {
            setAsUnCheckMessage(viewHolder, index)
        }
    }

    fun getDateStringForView(dateString: String): String {
        return getDateString(getDateFromString(dateString, FULL_DATE_PATTERN), TIME_DATE_PATTERN)
    }

    fun setAsCheckMessage(viewHolder: ViewHolder, index: Int) {
        checkedOrNotMessagesList[index] = true
        viewHolder.itemView.setBackgroundColor(context.resources.getColor(R.color.checked_message_background))
    }

    fun setAsUnCheckMessage(viewHolder: ViewHolder, index: Int) {
        checkedOrNotMessagesList[index] = false
        viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT)
    }

    fun onCheckMessage(viewHolder: ViewHolder, index: Int) {
        if (checkedOrNotMessagesList[index]) {
            setAsUnCheckMessage(viewHolder, index)
            checkedMessagesList.remove(messagesList[index])
        } else {
            setAsCheckMessage(viewHolder, index)
            checkedMessagesList.add(messagesList[index])
        }
        checkableMessages.onCheckMessage()
    }

    fun isOwnMessage(index: Int): Boolean {
        return messagesList[index].senderUId == currentUserId
    }

    override fun getItemViewType(position: Int): Int {
        if (isOwnMessage(position)) {
            return R.layout.own_message_layout
        } else {
            return R.layout.companion_message_layout
        }
    }

    inner class ViewHolder(view: View, viewType: Int) : RecyclerView.ViewHolder(view), View.OnClickListener,
        View.OnLongClickListener {
        val messageTextView: TextView
        val dateTextView: TextView
        val indicator: View?
        val profilePhotoView: CircleImageView
        val messageImageView: ImageView

        init {
            messageTextView = view.message_text
            profilePhotoView = view.profile_photo
            messageImageView = view.message_image
            dateTextView = view.findViewById(R.id.date_text) as TextView
            if (viewType == R.layout.own_message_layout) {
                indicator = view.indicator
            } else {
                indicator = null
            }
            this.itemView.setOnClickListener(this)
            this.itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            val message = messagesList[adapterPosition]
            if (checkedMessagesList.isNotEmpty()) {
                onCheckMessage(this, adapterPosition)
            }else if(!message.imageUrl.equals("")){
                showImage(message.imageUrl)
            }
        }

        fun showImage(imageUrl: String){
            val intent = Intent(context,ImageActivity::class.java)
            intent.putExtra(ImageActivity.IMAGE_URL, imageUrl)
            context.startActivity(intent)
        }

        override fun onLongClick(v: View?): Boolean {
            onCheckMessage(this, adapterPosition)
            return true
        }

        fun setMessageImage(url: String) {
            messageTextView.visibility = View.GONE
            messageImageView.visibility = View.VISIBLE
            Glide.with(context).load(url)
                .into(messageImageView)
        }

        fun setMessageText(text: String) {
            messageImageView.visibility = View.GONE
            messageTextView.visibility = View.VISIBLE
            messageTextView.text = text
        }
    }


    interface ICheckableMessages {
        fun onCheckMessage()
    }
}