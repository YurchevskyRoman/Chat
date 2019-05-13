package com.romanyu.chat

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.romanyu.chat.UserUtil.User
import com.romanyu.chat.adapters.UsersAdapter
import kotlinx.android.synthetic.main.activity_search_user.*

class SearchUserActivity : AppCompatActivity() {

    var searchMenuItem: MenuItem? = null
    var closeMenuItem: MenuItem? = null
    lateinit var usersAdapter: UsersAdapter
    val usersList: MutableList<User> = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_user)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        usersAdapter = UsersAdapter(this, usersList)
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        users_recycler.adapter = usersAdapter
        users_recycler.layoutManager = linearLayoutManager
        readUsersData()
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
            android.R.id.home -> {
                if (search_text.visibility == View.VISIBLE) {
                    search_text.visibility = View.GONE
                    closeMenuItem?.setVisible(false)
                    searchMenuItem?.setVisible(true)
                    return true
                } else {
                    return super.onOptionsItemSelected(item)
                }
            }
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

    fun readUsersData() {
        FirebaseDatabase.getInstance().reference.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                usersList.clear()
                val currentUser = FirebaseAuth.getInstance().currentUser
                dataSnapshot.children.forEach {
                    val user = it.child("UserInfo").getValue(User::class.java)
                    if (user != null && user.userId != currentUser?.uid) {
                        usersList.add(user)
                    }
                }
                usersAdapter.notifyDataSetChanged()
                users_progress_bar.visibility = View.GONE
                if(usersList.isEmpty()){
                    empty_content_warning.visibility = View.VISIBLE
                }else{
                    empty_content_warning.visibility = View.GONE
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }
}
