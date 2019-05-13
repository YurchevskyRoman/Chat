package com.romanyu.chat

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.*
import com.romanyu.chat.UserUtil.ChatInfoBlock
import com.romanyu.chat.UserUtil.User
import com.romanyu.chat.adapters.ChatsAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.view.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    lateinit var headerView: View
    var searchMenuItem: MenuItem? = null
    var closeMenuItem: MenuItem? = null
    var user: User? = null
    lateinit var databaseReference: DatabaseReference
    val chatsList: MutableList<ChatInfoBlock> = ArrayList<ChatInfoBlock>()

    val valuesEventsListenersList: MutableList<ValueEventListener> = java.util.ArrayList<ValueEventListener>()
    val referencesOnValuesEventsListenersList: MutableList<DatabaseReference> = java.util.ArrayList<DatabaseReference>()

    lateinit var chatsAdapter: ChatsAdapter

    companion object {
        val IMAGE_REQUEST = 0
    }

    lateinit var storageReference: StorageReference
    var imageUri : Uri? = null
    var uploadTask : UploadTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        databaseReference = FirebaseDatabase.getInstance().reference
        storageReference = FirebaseStorage.getInstance().reference
        setSupportActionBar(toolbar)
        val toogle: ActionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            R.string.nav_open_drawer,
            R.string.nav_close_drawer
        )
        drawer.addDrawerListener(toogle)
        toogle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        headerView = LayoutInflater.from(this).inflate(R.layout.nav_header, null)
        headerView.camera_button.setOnClickListener(this)
        nav_view.addHeaderView(headerView)
        chatsAdapter = ChatsAdapter(this, chatsList)
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        chats_recycler.adapter = chatsAdapter
        chats_recycler.layoutManager = linearLayoutManager
    }

    override fun onStart() {
        super.onStart()
        readDatas()
    }

    override fun onStop() {
        stopValueEventListener()
        super.onStop()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.camera_button -> {
                openImage()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val mUploadTask = uploadTask
        if(requestCode == IMAGE_REQUEST && resultCode == Activity.RESULT_OK
            && data != null && data.data != null){

            imageUri = data.data

            if(mUploadTask != null && mUploadTask.isInProgress){
                Toast.makeText(this, "Upload in progress", Toast.LENGTH_LONG).show()
            }else{
                uploadImage()
            }
        }
    }

    fun openImage(){
        val intent = Intent()
        intent.setType("image/*")
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, IMAGE_REQUEST)
    }

    fun getFileExtension(uri: Uri) : String{
        val contentResolver = this.contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver?.getType(uri)) as String
    }

    fun uploadImage(){
        val mImageUri = imageUri
        if(mImageUri != null){
            val fileName = System.currentTimeMillis().toString() + "." + getFileExtension(mImageUri)
            val fileReference = storageReference.child("avatarPhoto").child(fileName)
            uploadTask = fileReference.putFile(mImageUri)
            val mUploadTask = uploadTask

            mUploadTask?.addOnSuccessListener(
                object : OnSuccessListener<UploadTask.TaskSnapshot>{
                    override fun onSuccess(taskSnapshot: UploadTask.TaskSnapshot?) {
                        if (taskSnapshot?.metadata != null) {
                            if (taskSnapshot.metadata?.reference != null) {
                                val result = taskSnapshot.storage.downloadUrl
                                result.addOnSuccessListener(
                                    object : OnSuccessListener<Uri> {
                                        override fun onSuccess(uri: Uri?) {
                                            val downloadUrl = uri.toString()
                                            val mUser = user as User
                                            deletePhotoFromStorageByUrl(mUser.imageUrl)
                                            val map : HashMap<String, Any> = HashMap()
                                            map.put("imageUrl",downloadUrl)
                                            val reference = databaseReference.child("Users").child(mUser.userId)
                                                .child("UserInfo")
                                            reference.updateChildren(map)
                                            rewritePhotoUrl(downloadUrl)
                                        }
                                });
                            }
                        }
                    }
                }
            )
        }
    }

    fun deletePhotoFromStorageByUrl(photoUrl: String){
        val mStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl)
        mStorageReference.delete()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        searchMenuItem = menu?.findItem(R.id.search_item)
        searchMenuItem?.setVisible(true)
        closeMenuItem = menu?.findItem(R.id.close_item)
        closeMenuItem?.setVisible(false)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.search_item -> {
                search_text.visibility = View.VISIBLE
                searchMenuItem?.setVisible(false)
                closeMenuItem?.setVisible(true)
                search_text.requestFocus()
                return true
            }
            R.id.close_item -> {
                search_text.setText("")
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.create_single_chat -> {
                val intent = Intent(this, SearchUserActivity::class.java)
                startActivity(intent)
            }
            R.id.sign_out ->{
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
        return true
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun readDatas() {
        readUserInfo()
    }

    fun readUserInfo() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid as String
        val reference = databaseReference.child("Users").child(currentUserId).child("UserInfo")
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                user = dataSnapshot.getValue(User::class.java)
                headerView.username.text = user?.username
                headerView.email.text = user?.email
                Glide.with(this@MainActivity)
                    .load(user?.imageUrl)
                    .placeholder(R.drawable.placeholder_avatar)
                    .into(headerView.profile_photo)
                headerView.loading_block.visibility = View.GONE
                headerView.user_info_block.visibility = View.VISIBLE
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
        reference.addValueEventListener(valueEventListener)
        addValueEventListener(reference, valueEventListener)
        readChats(currentUserId)
    }

    fun readChats(currentUserId: String) {
        val reference = databaseReference.child("Users").child(currentUserId).child("ChatInfoBlocks")
        val valueEventListener = object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                chatsList.clear()
                dataSnapshot.children.forEach {
                    val chatInfoBlock = it.getValue(ChatInfoBlock::class.java) as ChatInfoBlock
                    chatsList.add(chatInfoBlock)
                }
                notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
        reference.addValueEventListener(valueEventListener)
        addValueEventListener(reference, valueEventListener)
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

    fun notifyDataSetChanged() {
        if(chatsList.isEmpty()){
            empty_content_warning.visibility = View.VISIBLE
        }else{
            empty_content_warning.visibility = View.GONE
        }
        chats_progress_bar.visibility = View.GONE
        chatsAdapter = ChatsAdapter(this, chatsList)
        chats_recycler.adapter = chatsAdapter
    }

    fun rewritePhotoUrl(photoUrl: String){
        val currentUserId = user?.userId as String
        val companionsIdList = ArrayList<String>()
        chatsList.forEach {
            companionsIdList.add(it.companionId)
        }
        rewriteAllPhotoUrlInChatInfoBlocks(photoUrl, currentUserId, companionsIdList)
    }

    fun rewriteAllPhotoUrlInChatInfoBlocks(photoUrl: String, userId: String, companionsIdList: ArrayList<String>){
        companionsIdList.forEach {
            rewritePhotoUrlForChat(photoUrl, userId, it)
        }
    }

    fun rewritePhotoUrlForChat(photoUrl: String, userId: String, companionId: String){
        val mapForCurrentUser : HashMap<String, Any> = HashMap()
        mapForCurrentUser.put("currentUserProfilePhotoUrl",photoUrl)
        databaseReference.child("Users").child(userId).child("ChatInfoBlocks")
            .child(companionId).updateChildren(mapForCurrentUser)
        val mapForCompanion : HashMap<String, Any> = HashMap()
        mapForCompanion.put("companionProfilePhotoUrl",photoUrl)
        databaseReference.child("Users").child(companionId).child("ChatInfoBlocks")
            .child(userId).updateChildren(mapForCompanion)
    }
}
