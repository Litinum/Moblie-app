package com.example.skucise

import android.annotation.SuppressLint
import java.time.LocalDateTime

@SuppressLint("NewApi")
data class User(
    var id : Int,
    var username : String = "",
    var password : String = "",
    var email : String = "",
    var firstname : String = "",
    var lastname : String = "",
    val creationDate : LocalDateTime = LocalDateTime.now(),
    val numberOfAdverts : Int = 0,
    val averageRating: String = ""
)