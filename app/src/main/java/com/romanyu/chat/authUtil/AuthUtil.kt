package com.romanyu.chat.authUtil

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.romanyu.chat.MainActivity
import com.romanyu.chat.dialog.VerifyEmailDialog
import java.util.regex.Pattern

val emailRegex:String = "(\\w+@[a-zA-Z_]+?\\.[a-zA-Z]{2,6})"

fun isValidEmail(email:String):Boolean{
    var isValid = true
    if(!Pattern.matches(emailRegex,email)){
        isValid = false
    }
    return isValid
}

fun isValidPassword(password:String):Boolean{
    var isValid = true
    if(password.length<8 || password.length>32 || !checkPasswordByLetter(password) ){
        isValid = false
    }
    return isValid
}

fun isValidUsername(username:String):Boolean{
    var isValid = true
    if(username.length<3 || username.length>32 || !checkUsernameByLetter(username)){
        isValid = false
    }
    return isValid
}

fun checkPasswordByLetter(password:String):Boolean{
    val passwordLetters:CharArray =  password.toCharArray()
    var isValid = true
    for (letter in passwordLetters){
        isValid = when(letter){
            in 'A'..'Z',in 'a'..'z',in '0'..'9','_','.' -> true
            else -> false
        }
        if(!isValid){
            break
        }
    }
    return isValid
}

fun checkUsernameByLetter(username:String):Boolean{
    val usernameLetters:CharArray = username.toCharArray()
    var isValid = true
    for(letter in usernameLetters){
        isValid = when(letter){
            in 'A'..'Z',in 'a'..'z',in '0'..'9','_' -> true
            else -> false
        }
        if(!isValid){
            break
        }
    }
    return isValid
}

//Autorisation function

fun signInCompleted(activity: Activity){
    val intent = Intent(activity, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
    activity.startActivity(intent)
}

fun isEmailVerify():Boolean{
    val mUser = FirebaseAuth.getInstance().currentUser
    return mUser?.isEmailVerified  ?: false
}