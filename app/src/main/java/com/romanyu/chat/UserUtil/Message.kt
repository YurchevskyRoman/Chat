package com.romanyu.chat.UserUtil
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Message(
    var messageKey:String = "",
    val senderUId:String = "",
    val recipientUId:String = "",
    val imageUrl:String = "",
    val fileUrl:String = "",
    val text:String = "",
    val date:String = "",
    @field:JvmField
    val isRead:Boolean = false,
    @field:JvmField
    val isShowOnlyForSender: Boolean = false
) {
    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "messageKey" to messageKey,
            "senderUId" to senderUId,
            "recipientUId" to recipientUId,
            "imageUrl" to imageUrl,
            "fileUrl" to fileUrl,
            "text" to text,
            "isRead" to isRead,
            "isShowOnlyForSender" to isShowOnlyForSender,
            "date" to date
        )
    }
}