package com.romanyu.chat

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.romanyu.chat.UserUtil.ChatInfoBlock
import com.romanyu.chat.UserUtil.Message
import com.romanyu.chat.UserUtil.User
import com.romanyu.chat.adapters.MessagesAdapter
import com.romanyu.chat.authUtil.FULL_DATE_PATTERN
import com.romanyu.chat.authUtil.getDateString
import kotlinx.android.synthetic.main.activity_single_chat.*
import java.util.*
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.database.Cursor
import android.net.Uri
import android.widget.Toast
import com.romanyu.chat.gallery.ImageData
import android.provider.MediaStore
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.MimeTypeMap
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.romanyu.chat.gallery.ImagesAdapter
import java.io.File
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class SingleChatActivity : AppCompatActivity(), TextWatcher, View.OnClickListener,
    MessagesAdapter.ICheckableMessages, ImagesAdapter.IShowableSendButton {

    companion object {
        val COMPANION_USER_ID = "COMPANION_USER_ID"
        val PERMISSION_READ_EXTERNAL_STORAGE = 0
        var lastDeleteMessageId: Int? = null
    }

    var companion: User? = null
    var currentUser: User? = null
    var chatKey: String? = null
    var companionId: String? = null
    lateinit var databaseReference: DatabaseReference


    //Lists
    val messagesList: MutableList<Message> = ArrayList<Message>()
    val checkedMessagesList: MutableList<Message> = ArrayList<Message>()
    lateinit var imagesList: MutableList<ImageData>
    val checkedOrNotMessagesList: MutableList<Boolean> = ArrayList<Boolean>()
    val valuesEventsListenersList: MutableList<ValueEventListener> = ArrayList<ValueEventListener>()
    val referencesOnValuesEventsListenersList: MutableList<DatabaseReference> = ArrayList<DatabaseReference>()
    val menuItemsList: MutableList<MenuItem> = ArrayList<MenuItem>()
    val userProfilePhotos: HashMap<String, Any> = HashMap()

    lateinit var messagesAdapter: MessagesAdapter
    var messagesManipulationSettingsIsVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_chat)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        message_text.requestFocus()
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager.stackFromEnd = true
        messages_recycler.layoutManager = linearLayoutManager
        databaseReference = FirebaseDatabase.getInstance().reference
        companionId = intent.extras.getString(COMPANION_USER_ID)
        message_text.addTextChangedListener(this)
        send_button.setOnClickListener(this)
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        gallery.layoutManager = layoutManager
        attach_photo_button.setOnClickListener(this)
        gallery_hide_button.setOnClickListener(this)
        gallery_block.setOnClickListener(this)
        gallery_send_button.setOnClickListener(this)
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
                sendTextMessage()
            }
            R.id.attach_photo_button -> {
                onClickAttachPhotoButton()
            }
            R.id.gallery_hide_button -> {
                hideGallery()
            }
            R.id.gallery_block -> {
                hideGallery()
            }
            R.id.gallery_send_button -> {
                sendImageMessage()
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
                replyMessages()
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

    fun isOwnMessage(message: Message): Boolean {
        return message.senderUId.equals(currentUser?.userId)
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

    fun sendTextMessage() {
        val text = message_text.text.toString()
        message_text.setText("")
        packUpMessageAndSend(text = text)
    }

    fun packUpMessageAndSend(
        text: String = "", imageUrl: String = "", fileUrl: String = "",
        senderUId: String = currentUser?.userId ?: "", recipientUId: String = companion?.userId ?: ""
    ) {
        if (companion != null && currentUser != null) {
            val date = getDateString(Date(), FULL_DATE_PATTERN)
            val message = Message(
                senderUId = senderUId,
                recipientUId = recipientUId,
                text = text,
                imageUrl = imageUrl,
                fileUrl = fileUrl,
                date = date
            )
            sendMessage(message)
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
            var decrement = 0
            checkedMessagesList.forEach {
                if (isOwnMessage(it)) {
                    if (!it.imageUrl.equals("")) {
                        deletePhotoFromStorageByUrl(it.imageUrl)
                    }
                    reference.child(it.messageKey).removeValue()
                    if (!it.isRead) {
                        decrement++
                    }
                } else {
                    reference.child(it.messageKey).child("isShowOnlyForSender").setValue(true)
                }
            }
            if (decrement > 0) {
                decrementUnreadMessages(decrement)
            }
        }
        hideMessagesManipulationSettings()
    }

    fun deletePhotoFromStorageByUrl(photoUrl: String) {
        val mStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl)
        mStorageReference.delete()
    }

    fun closeMessagesMenuItemHandler() {
        hideMessagesManipulationSettings()
        checkedMessagesList.clear()
        resetCheckedOrNotMessagesList()
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        val mUserId = currentUser?.userId as String
        val mCompanionId = companionId as String
        val mChatKey = chatKey as String
        val reference = databaseReference.child("SinglesChats").child(mChatKey).child("Messages").push()
        val messageKey = reference.key
        message.messageKey = messageKey ?: ""
        incrementUnreadMessages()
        reference.setValue(message.toMap())
        val usersId: Array<String> = arrayOf(mUserId, mCompanionId)
        setSameLastMessageForAll(usersId, message)

    }

    fun replyMessages() {
        checkedMessagesList.forEach {
            packUpMessageAndSend(
                text = it.text,
                imageUrl = it.imageUrl,
                fileUrl = it.fileUrl,
                recipientUId = it.recipientUId
            )
        }
    }

    fun setChatProperties(chatKey: String, usersId: Array<String>, allMessages: Array<Message>) {
        val messages = getLastMessageForAll(usersId, allMessages)
        setLastMessageForAll(usersId, messages)
        resetUnreadMessagesAmountForCurrentUser()
    }

    fun setSameLastMessageForAll(usersId: Array<String>, message: Message) {
        for (id in usersId) {
            var mCompanionId: String;
            if (id.equals(message.recipientUId)) {
                mCompanionId = message.senderUId
            } else {
                mCompanionId = message.recipientUId
            }
            setLastMessage(id, mCompanionId, message)
        }
    }

    fun setLastMessageForAll(usersId: Array<String>, messages: Array<Message>) {
        setLastMessage(usersId[0], usersId[1], messages[0])
        setLastMessage(usersId[1], usersId[0], messages[1])
    }

    fun setLastMessage(currentUserId: String, companionId: String, message: Message) {
        databaseReference.child("Users").child(currentUserId).child("ChatInfoBlocks")
            .child(companionId).child("lastMessage").setValue(message.toMap())
    }

    fun getLastMessageForAll(usersId: Array<String>, messages: Array<Message>): Array<Message> {
        val lastMessages = Array<Message>(usersId.size, { Message(isRead = true) })
        val states = Array<Boolean>(usersId.size, { false })
        val upperMessageIndex = messages.size - 1
        for (messageIndex in upperMessageIndex downTo 0) {
            for (userIdIndex in 0 until usersId.size) {
                if (!states[userIdIndex] && isMessageOwnedByUser(usersId[userIdIndex], messages[messageIndex])) {
                    lastMessages[userIdIndex] = messages[messageIndex]
                    states[userIdIndex] = true
                }
            }
            if (isLastMessagesToPack(states)) {
                break
            }
        }
        return lastMessages
    }

    fun isLastMessagesToPack(states: Array<Boolean>): Boolean {
        states.forEach {
            if (it == false) {
                return false
            }
        }
        return true
    }

    fun isMessageDontOwnedByUser(userId: String, message: Message): Boolean {
        val isSender = message.senderUId.equals(userId)
        return !isSender && message.isShowOnlyForSender
    }

    fun isMessageOwnedByUser(userId: String, message: Message): Boolean {
        return !isMessageDontOwnedByUser(userId, message)
    }

    fun incrementUnreadMessages(i: Int = 1) {
        val mCompanionId = companionId as String
        val mCurrentUserId = currentUser?.userId as String
        databaseReference.child("Users").child(mCompanionId).child("ChatInfoBlocks")
            .child(mCurrentUserId).child("unReadMessagesAmount")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var amount = dataSnapshot.getValue(Int::class.java) as Int
                    amount += i
                    setUnreadMessagesAmount(mCompanionId, mCurrentUserId, amount)
                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            });
    }

    fun decrementUnreadMessages(i: Int = 1) {
        val mCompanionId = companionId as String
        val mCurrentUserId = currentUser?.userId as String
        databaseReference.child("Users").child(mCompanionId).child("ChatInfoBlocks")
            .child(mCurrentUserId).child("unReadMessagesAmount")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var amount = dataSnapshot.getValue(Int::class.java) as Int
                    amount -= i
                    setUnreadMessagesAmount(mCompanionId, mCurrentUserId, amount)
                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            });
    }

    fun setUnreadMessagesAmount(currentUserId: String, companionId: String, amount: Int) {
        databaseReference.child("Users").child(currentUserId).child("ChatInfoBlocks")
            .child(companionId).child("unReadMessagesAmount").setValue(amount)
    }

    fun createChatInfoBlockForParticipants(chatKey: String, currentUser: User, companion: User) {
        val currentUserChatInfoBlock = ChatInfoBlock(
            companionProfilePhotoUrl = companion.imageUrl,
            currentUserProfilePhotoUrl = currentUser.imageUrl,
            companionUserName = companion.username,
            chatKey = chatKey,
            companionId = companion.userId
        )
        val companionChatInfoBlock = ChatInfoBlock(
            companionProfilePhotoUrl = currentUser.imageUrl,
            currentUserProfilePhotoUrl = companion.imageUrl,
            companionUserName = currentUser.username,
            chatKey = chatKey,
            companionId = currentUser.userId
        )
        createChatInfoBlockForUser(currentUserChatInfoBlock, currentUser.userId, companion.userId)
        createChatInfoBlockForUser(companionChatInfoBlock, companion.userId, currentUser.userId)
    }

    fun createChatInfoBlockForUser(userChatInfoBlock: ChatInfoBlock, currentUserId: String, companionId: String) {
        databaseReference.child("Users").child(currentUserId).child("ChatInfoBlocks").child(companionId)
            .setValue(userChatInfoBlock.toMap())
    }

    fun createChat(message: Message) {
        val reference = databaseReference.child("SinglesChats").push()
        chatKey = reference.key
        val participantsReference = reference.child("Participants")
        val currentUserId = currentUser?.userId as String
        val companionId = companion?.userId as String
        participantsReference.child("participantA").setValue(currentUserId)
        participantsReference.child("participantB").setValue(companionId)
        databaseReference.child("Users").child(currentUserId).child("SinglesChats").push().child("chatKey")
            .setValue(chatKey)
        databaseReference.child("Users").child(companionId).child("SinglesChats").push().child("chatKey")
            .setValue(chatKey)
        val mChatKey = chatKey as String
        val mCurrentUser = currentUser as User
        val mCompanion = companion as User
        createChatInfoBlockForParticipants(mChatKey, mCurrentUser, mCompanion)
        addMessage(message)
    }

    fun readDatas(companionId: String) {
        readCurrentUserData(companionId)
    }

    fun readChatKey() {
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
        val mChatKey = chatKey as String
        val mCompanionId = companion?.userId as String
        val mCurrentUserId = currentUser?.userId as String
        val reference = databaseReference.child("SinglesChats").child(mChatKey).child("Messages")
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val allMessages = ArrayList<Message>()
                messagesList.clear()
                checkedMessagesList.clear()
                checkedOrNotMessagesList.clear()
                userProfilePhotos.clear()
                dataSnapshot.children.forEach {
                    val message = it.getValue(Message::class.java) as Message
                    allMessages.add(message)
                    if (isMessageShow(message)) {
                        val senderUId = message.senderUId
                        if (!userProfilePhotos.containsKey(senderUId)) {
                            userProfilePhotos.put(senderUId, "")
                        }
                        if (message.senderUId == mCompanionId && !message.isRead) {
                            setAsReadMessage(message)
                        }
                        messagesList.add(message)
                        checkedOrNotMessagesList.add(false)
                    }
                }
                val usersId = arrayOf(mCurrentUserId, mCompanionId)
                setChatProperties(mChatKey, usersId, allMessages.toTypedArray())
                readSenderProfilePhotoUrls()
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
        reference.addValueEventListener(valueEventListener)
        addValueEventListener(reference, valueEventListener)
    }

    fun readSenderProfilePhotoUrls() {
        if(userProfilePhotos.isEmpty()){
            empty_content_warning.visibility = View.VISIBLE
            messages_progress_bar.visibility = View.GONE
            return
        }
        userProfilePhotos.forEach {
            getSenderProfilePhotoUrl(it.key)
        }

    }

    fun getSenderProfilePhotoUrl(userId: String) {
        databaseReference.child("Users").child(userId).child("UserInfo").child("imageUrl")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val imageUrl = dataSnapshot.getValue(String::class.java) as String
                        userProfilePhotos.set(userId, imageUrl)
                        if (isSenderProfilePhotoUrlPackUp()) {
                            notifyDataSetChanged()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                }
            )
    }

    fun isSenderProfilePhotoUrlPackUp(): Boolean {
        val values = userProfilePhotos.values
        values.forEach {
            if (it.equals("")) {
                return false
            }
        }
        return true
    }


    fun setAsReadMessage(message: Message) {
        val key = chatKey
        if (key != null) {
            databaseReference.child("SinglesChats").child(key).child("Messages")
                .child(message.messageKey)
                .child("isRead").setValue(true)
        }
    }

    fun resetUnreadMessagesAmountForCurrentUser() {
        val mCurrentUserId = currentUser?.userId as String
        val mCompanionId = companionId as String
        setUnreadMessagesAmount(mCurrentUserId, mCompanionId, 0)
    }

    fun isMesssageDontShow(message: Message): Boolean {
        return !isOwnMessage(message) && message.isShowOnlyForSender
    }

    fun isMessageShow(message: Message): Boolean {
        return !isMesssageDontShow(message)
    }

    fun notifyDataSetChanged() {
        messages_progress_bar.visibility = View.GONE
        empty_content_warning.visibility = View.GONE
        messagesAdapter =
            MessagesAdapter(this, messagesList, checkedMessagesList, checkedOrNotMessagesList, userProfilePhotos)
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

    ///////////////////////////////////////////////////////////////////////////////////////

    fun checkPermissionForGetImages() {
        if (isPermissionAccess(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showImagesList();
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_READ_EXTERNAL_STORAGE
            );
        }
    }

    fun isPermissionAccess(permissions: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissions) == PackageManager.PERMISSION_GRANTED
    }


    private fun showImagesList() {
        imagesList = getAllImagesFromDevice()
        val imagesAdapter = ImagesAdapter(this, imagesList)
        gallery.setAdapter(imagesAdapter)
        showGallery()
    }

    private fun getAllImagesFromDevice(): MutableList<ImageData> {
        val imagesList = ArrayList<ImageData>()
        val uri: Uri
        val cursor: Cursor?
        val column_index: Int
        var path: String? = null
        val sortOrder: String
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        sortOrder = MediaStore.Images.ImageColumns.DATE_ADDED + " DESC"
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }
        cursor = this.contentResolver.query(uri, projection, null, null, sortOrder)
        try {
            if (null != cursor) {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                while (cursor.moveToNext()) {
                    path = cursor.getString(column_index)
                    val imageData = ImageData(path)
                    imagesList.add(imageData)
                }
                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return imagesList
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_READ_EXTERNAL_STORAGE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showImagesList()
                } else {
                    Toast.makeText(this, "This app do not have access to storage!!!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun onClickAttachPhotoButton() {
        checkPermissionForGetImages()
    }

    fun showGallery() {
        gallery_block.visibility = View.VISIBLE
        val showBottomSheetAnimation = AnimationUtils.loadAnimation(
            this, R.anim.gallery_show
        )
        val appearGalleryBlockAnimation = AnimationUtils.loadAnimation(
            this, R.anim.gallery_block_appear
        )
        gallery_block.startAnimation(appearGalleryBlockAnimation)
        bottom_sheet.startAnimation(showBottomSheetAnimation)
    }

    fun hideGallery() {
        val hideBottomSheetAnimation = AnimationUtils.loadAnimation(
            this, R.anim.gallery_hide
        )
        val fadeGalleryBlockAnimation = AnimationUtils.loadAnimation(
            this, R.anim.gallery_block_fade
        )
        fadeGalleryBlockAnimation.setAnimationListener(
            object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {

                }

                override fun onAnimationRepeat(animation: Animation?) {

                }

                override fun onAnimationEnd(animation: Animation?) {
                    gallery_block.visibility = View.GONE
                    hideButton()
                }
            }
        )
        gallery_block.startAnimation(fadeGalleryBlockAnimation)
        bottom_sheet.startAnimation(hideBottomSheetAnimation)
    }

    fun sendImageMessage() {
        hideGallery()
        imagesList.forEach {
            if (it.isCheck) {
                val imageUri = Uri.fromFile(File(it.imagePath))
                if (imageUri != null) {
                    uploadAndSendImage(imageUri)
                }
            }
        }
    }

    fun getFileExtension(uri: Uri): String {
        val uriParts = uri.toString().split('.')
        val fileExtension = uriParts[uriParts.size - 1]
        Log.d("ryu", fileExtension.toString())
        return fileExtension
    }

    fun uploadAndSendImage(imageUri: Uri?) {
        val mImageUri = imageUri
        if (mImageUri != null) {
            val fileName = System.currentTimeMillis().toString() + "." + getFileExtension(mImageUri)
            val fileReference = FirebaseStorage.getInstance().reference.child("messageImages").child(fileName)
            val mUploadTask = fileReference.putFile(mImageUri)

            mUploadTask.addOnSuccessListener(
                object : OnSuccessListener<UploadTask.TaskSnapshot> {
                    override fun onSuccess(taskSnapshot: UploadTask.TaskSnapshot?) {
                        if (taskSnapshot?.metadata != null) {
                            if (taskSnapshot.metadata?.reference != null) {
                                val result = taskSnapshot.storage.downloadUrl
                                result.addOnSuccessListener(
                                    object : OnSuccessListener<Uri> {
                                        override fun onSuccess(uri: Uri?) {
                                            val downloadUrl = uri.toString()
                                            packUpMessageAndSend(imageUrl = downloadUrl)
                                        }
                                    });
                            }
                        }
                    }
                }
            )
        }
    }

    override fun showButton() {
        gallery_send_button.visibility = View.VISIBLE
        gallery_hide_button.visibility = View.GONE
    }

    override fun hideButton() {
        gallery_send_button.visibility = View.GONE
        gallery_hide_button.visibility = View.VISIBLE
        ImageData.resetNumber()
    }
}
