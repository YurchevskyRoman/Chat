package com.romanyu.chat.UserUtil

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val username:String = "",
    val email:String = "",
    val imageUrl:String = "https://firebasestorage.googleapis.com/v0/b/chat-73ee2.appspot.com/o/placeholder_avatar.jpg?alt=media&token=e33febf7-3677-4643-8e5f-41500e18a743",
    val userId:String = ""
) {
    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "username" to username,
            "email" to email,
            "imageUrl" to imageUrl,
            "userId" to userId
        )
    }
}