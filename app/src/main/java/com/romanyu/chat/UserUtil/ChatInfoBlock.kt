package com.romanyu.chat.UserUtil

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties


@IgnoreExtraProperties
data class ChatInfoBlock(
    var companionProfilePhotoUrl:String = "",
    var currentUserProfilePhotoUrl:String = "",
    var companionUserName:String = "",
    var lastMessage:Message = Message(),
    var unReadMessagesAmount:Int = 0,
    var chatKey:String = "",
    var companionId: String = ""
) {
    @Exclude
    fun toMap() : Map<String, Any>{
        return mapOf(
            "companionProfilePhotoUrl" to companionProfilePhotoUrl,
            "currentUserProfilePhotoUrl" to currentUserProfilePhotoUrl,
            "companionUserName" to companionUserName,
            "lastMessage" to lastMessage,
            "unReadMessagesAmount" to unReadMessagesAmount,
            "chatKey" to chatKey,
            "companionId" to companionId
        )
    }
}