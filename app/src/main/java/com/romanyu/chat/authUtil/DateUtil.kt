package com.romanyu.chat.authUtil

import java.text.SimpleDateFormat
import java.util.*

val FULL_DATE_PATTERN = "yyyy.MM.dd G HH:mm:ss"
val TIME_DATE_PATTERN = "HH:mm"

fun getDateString(date: Date, pattern: String) : String{
    val dateFormat = SimpleDateFormat(pattern, Locale.ENGLISH)
    val dateString = dateFormat.format(date) as String
    return dateString
}

fun getDateFromString(dateString: String, pattern: String) : Date{
    val dateFormat = SimpleDateFormat(pattern, Locale.ENGLISH)
    val date = dateFormat.parse(dateString) as Date
    return date
}