package com.romanyu.chat

import android.content.Intent
import android.content.res.ColorStateList
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.romanyu.chat.UserUtil.User
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.view.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var headerView: View
    var searchMenuItem: MenuItem? = null
    var closeMenuItem: MenuItem? = null
    var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val toogle:ActionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            R.string.nav_open_drawer,
            R.string.nav_close_drawer
        )
        drawer.addDrawerListener(toogle)
        toogle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        headerView = LayoutInflater.from(this).inflate(R.layout.nav_header,null)
        nav_view.addHeaderView(headerView)
        readUserInfo()
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
        when(menuItem.itemId){
            R.id.create_single_chat -> {
                val intent = Intent(this, SearchUserActivity::class.java)
                startActivity(intent)
            }
        }
        return true
    }

    override fun onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)){
            drawer.closeDrawer(GravityCompat.START)
        }else{
            super.onBackPressed()
        }
    }

    fun readUserInfo(){
        val currentUser = FirebaseAuth.getInstance().currentUser
        if(currentUser != null){
            FirebaseDatabase.getInstance().reference.child("Users").child(currentUser.uid).child("UserInfo")
                .addValueEventListener(
                    object : ValueEventListener{
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
                )
        }
    }
}
