package com.romanyu.chat

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_image.*

class ImageActivity : AppCompatActivity() {

    companion object {
        val IMAGE_URL = "IMAGE_URL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        toolbar.setPadding(0, getStatusBarHeight(), 0, 0)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        val imageUrl = intent.extras.getString(IMAGE_URL)
        loadingImage(imageUrl)
    }

    fun loadingImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .into(image_container)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return true
    }

    fun getStatusBarHeight() : Int{
        val height: Int
        val heightId = resources.getIdentifier(
            "status_bar_height",
            "dimen",
            "android"
        )
        height = resources.getDimensionPixelSize(heightId)
        return height
    }
}
