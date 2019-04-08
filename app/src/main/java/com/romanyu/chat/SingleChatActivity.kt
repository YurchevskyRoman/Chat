package com.romanyu.chat

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.romanyu.chat.UserUtil.Message
import com.romanyu.chat.UserUtil.User
import com.romanyu.chat.adapters.MessagesAdapter
import kotlinx.android.synthetic.main.activity_single_chat.*
import kotlin.math.log

class SingleChatActivity : AppCompatActivity(), TextWatcher, View.OnClickListener, MessagesAdapter.ICheckableMessages {

    companion object {
        val COMPANION_USER_ID = "COMPANION_USER_ID"
    }

    var companion: User? = null
    var currentUser: User? = null
    var chatKey: String? = null
    var companionId: String? = null
    lateinit var databaseReference: DatabaseReference


    //Lists
    val messagesList: MutableList<Message> = ArrayList<Message>()
    val checkedMessagesList: MutableList<Message> = ArrayList<Message>()
    val checkedOrNotMessagesList: MutableList<Boolean> = ArrayList<Boolean>()
    val valuesEventsListenersList: MutableList<ValueEventListener> = ArrayList<ValueEventListener>()
    val referencesOnValuesEventsListenersList: MutableList<DatabaseReference> = ArrayList<DatabaseReference>()
    val menuItemsList: MutableList<MenuItem> = ArrayList<MenuItem>()

    lateinit var messagesAdapter: MessagesAdapter
    var messagesManipulationSettingsIsVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_chat)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        messagesAdapter = MessagesAdapter(this, messagesList, checkedMessagesList, checkedOrNotMessagesList)
        message_text.requestFocus()
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager.stackFromEnd = true
        messages_recycler.adapter = messagesAdapter
        messages_recycler.layoutManager = linearLayoutManager
        databaseReference = FirebaseDatabase.getInstance().reference
        companionId = intent.extras.getString(COMPANION_USER_ID)
        message_text.addTextChangedListener(this)
        send_button.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        val mCompanionId = companionId
        if (mCompanionId != null) {
            readDatas(mCompanionId)
        }
    }

    override fun onStop() {
        stopValueEventListener()
        super.onStop()
    }

    override fun afterTextChanged(s: Editable?) {

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s?.length ?: 0 > 0) {
            attach_photo_button.visibility = View.GONE
            attach_file_button.visibility = View.GONE
            send_button.visibility = View.VISIBLE
        } else {
            attach_file_button.visibility = View.VISIBLE
            attach_photo_button.visibility = View.VISIBLE
            send_button.visibility = View.GONE
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.send_button -> {
                val text = message_text.text.toString()
                message_text.setText("")
                if (companion != null && currentUser != null) {
                    val senderUId: String = currentUser?.userId ?: ""
                    val recipientUId: String = companion?.userId ?: ""
                    val message = Message(
                        senderUId = senderUId,
                        recipientUId = recipientUId,
                        text = text
                    )
                    sendMessage(message)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.messages_manipulation_menu, menu)
        val closeMenuItem = menu?.findItem(R.id.close_item)
        addMenuItem(closeMenuItem)
        val replyMenuItem = menu?.findItem(R.id.reply_item)
        addMenuItem(replyMenuItem)
        val forwardMenuItem = menu?.findItem(R.id.forward_item)
        addMenuItem(forwardMenuItem)
        val deleteMenuItem = menu?.findItem(R.id.delete_item)
        addMenuItem(deleteMenuItem)
        hideMenu()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.close_item -> {
                closeMessagesMenuItemHandler()
                return true
            }
            R.id.reply_item -> {
                return true
            }
            R.id.forward_item -> {
                return true
            }
            R.id.delete_item -> {
                deleteMessagesMenuItemHandler()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }

        }
    }

    fun setTitleToolbar(title: String) {
        supportActionBar?.title = title
    }

    fun addMenuItem(menuItem: MenuItem?) {
        if (menuItem != null) {
            menuItemsList.add(menuItem)
        }
    }

    fun hideMenu() {
        menuItemsList.forEach {
            it.setVisible(false)
        }
    }

    fun showMenu() {
        menuItemsList.forEach {
            it.setVisible(true)
        }
    }

    fun sendMessage(message: Message) {
        if (chatKey != null) {
            addMessage(message)
        } else {
            createChat(message)
        }
    }

    fun deleteMessagesMenuItemHandler() {
        val key = chatKey
        if (key != null) {
            val reference = FirebaseDatabase.getInstance().reference.child("SinglesChats").child(key).child("Messages")
            checkedMessagesList.forEach {
                reference.child(it.messageKey).removeValue()
            }
            checkedMessagesList.clear()
        }
        hideMessagesManipulationSettings()
    }

    fun closeMessagesMenuItemHandler() {
        hideMessagesManipulationSettings()
        checkedMessagesList.clear()
        resetCheckedOrNotMessagesList()
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        val key = chatKey
        if (key != null) {
            val reference = databaseReference.child("SinglesChats").child(key).child("Messages").push()
            val messageKey = reference.key
            message.messageKey = messageKey ?: ""
            Log.d("ryu", message.isRead.toString())
            reference.child("messageInfo").setValue(message.toMap())
        }
    }

    fun createChat(message: Message) {
        val reference = databaseReference.child("SinglesChats").push()
        chatKey = reference.key
        val participantsReference = reference.child("Participants")
        val currentUserId = currentUser?.userId
        val companionId = companion?.userId
        if (currentUserId != null && companionId != null) {
            participantsReference.child("participantA").setValue(currentUserId)
            participantsReference.child("participantB").setValue(companionId)
            databaseReference.child("Users").child(currentUserId).child("SinglesChats").push().child("chatKey")
                .setValue(chatKey)
            databaseReference.child("Users").child(companionId).child("SinglesChats").push().child("chatKey")
                .setValue(chatKey)
            addMessage(message)
        }
    }

    fun readDatas(companionId: String) {
        Log.d("ryu", "readDatas")
        readCurrentUserData(companionId)
    }

    fun readChatKey() {
        Log.d("ryu", "readChatKey")
        val currentUserId = currentUser?.userId
        if (currentUserId != null) {
            val reference = databaseReference.child("Users").child(currentUserId).child("SinglesChats")
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dataSnapshot.children.forEach {
                        val key = it.child("chatKey").getValue(String::class.java)
                        if (key != null) {
                            val mReference = databaseReference.child("SinglesChats").child(key).child("Participants")
                            val mValueEventListener = object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    val participantAUId =
                                        dataSnapshot.child("participantA").getValue(String::class.java)
                                    val participantBUId =
                                        dataSnapshot.child("participantB").getValue(String::class.java)
                                    if ((participantAUId == companion?.userId && participantBUId == currentUser?.userId)
                                        || (participantBUId == companion?.userId && participantAUId == currentUser?.userId)
                                    ) {
                                        chatKey = key
                                        Log.d("ryu", key)
                                        readMessages()
                                        return
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {

                                }
                            }
                            mReference.addValueEventListener(mValueEventListener)
                            addValueEventListener(mReference, mValueEventListener)
                        }
                    }
                }

                override fun onCancelled(dataSnapshot: DatabaseError) {

                }
            }
            reference.addValueEventListener(valueEventListener)
            addValueEventListener(reference, valueEventListener)
        }
    }

    fun readCurrentUserData(companionId: String) {
        Log.d("ryu", "readCurrentUserData")
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            val reference = databaseReference.child("Users").child(currentUserId).child("UserInfo")
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    currentUser = dataSnapshot.getValue(User::class.java)
                    readCompanionData(companionId)
                }

                override fun onCancelled(dataSnapshot: DatabaseError) {

                }
            }
            reference.addValueEventListener(valueEventListener)
            addValueEventListener(reference, valueEventListener)
        }
    }

    fun readCompanionData(companionId: String) {
        Log.d("ryu", "readCompanionData")
        val reference = databaseReference.child("Users").child(companionId).child("UserInfo")
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                companion = dataSnapshot.getValue(User::class.java)
                username.text = companion?.username
                email.text = companion?.email
                Glide.with(this@SingleChatActivity).load(companion?.imageUrl)
                    .placeholder(R.drawable.placeholder_avatar)
                    .into(profile_photo)
                readChatKey()
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {

            }
        }
        reference.addValueEventListener(valueEventListener)
        addValueEventListener(reference, valueEventListener)
    }

    fun readMessages() {
        Log.d("ryu", "readMessages")
        val key = chatKey
        if (key != null) {
            val reference = databaseReference.child("SinglesChats").child(key).child("Messages")
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    messagesList.clear()
                    checkedMessagesList.clear()
                    checkedOrNotMessagesList.clear()
                    dataSnapshot.children.forEach {
                        val message = it.child("messageInfo").getValue(Message::class.java)
                        Log.d("ryu", message.toString())
                        val companionId = companion?.userId
                        if (message != null && companionId != null) {
                            if (message.senderUId == companionId && !message.isRead) {
                                val key = chatKey
                                if (key != null) {
                                    databaseReference.child("SinglesChats").child(key).child("Messages")
                                        .child(message.messageKey).child("messageInfo")
                                        .child("isRead").setValue(true)
                                }
                            }
                            messagesList.add(message)
                            checkedOrNotMessagesList.add(false)
                        }
                    }
                    notifyDataSetChanged()
                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            }
            reference.addValueEventListener(valueEventListener)
            addValueEventListener(reference, valueEventListener)
        }
    }

    fun notifyDataSetChanged() {
        messagesAdapter = MessagesAdapter(this, messagesList, checkedMessagesList, checkedOrNotMessagesList)
        messages_recycler.adapter = messagesAdapter
    }

    fun addValueEventListener(reference: DatabaseReference, valueEventListener: ValueEventListener) {
        referencesOnValuesEventsListenersList.add(reference)
        valuesEventsListenersList.add(valueEventListener)
    }

    fun stopValueEventListener() {
        for (index in 0 until referencesOnValuesEventsListenersList.size) {
            referencesOnValuesEventsListenersList[index].removeEventListener(valuesEventsListenersList[index])
        }
        referencesOnValuesEventsListenersList.clear()
        valuesEventsListenersList.clear()
    }

    fun resetCheckedOrNotMessagesList() {
        for (index in 0 until checkedOrNotMessagesList.size) {
            checkedOrNotMessagesList[index] = false
        }
    }

    override fun onCheckMessage() {
        if (checkedMessagesList.isNotEmpty()) {
            if (!messagesManipulationSettingsIsVisible) {
                showMessagesManipulationSettings()
            }
            setTitleToolbar(checkedMessagesList.size.toString())
        } else {
            if (messagesManipulationSettingsIsVisible) {
                hideMessagesManipulationSettings()
            }
        }
    }

    fun showMessagesManipulationSettings() {
        messagesManipulationSettingsIsVisible = true
        showMenu()
        companion_header.visibility = View.GONE
    }

    fun hideMessagesManipulationSettings() {
        messagesManipulationSettingsIsVisible = false
        hideMenu()
        companion_header.visibility = View.VISIBLE
    }

}
