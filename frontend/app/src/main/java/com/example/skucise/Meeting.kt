package com.example.skucise

import java.time.LocalDateTime

data class Meeting(
    val id: Int,
    val advertId: Int,
    val otherUser: Int,
    val username: String,
    val title: String,
    var proposedTime: LocalDateTime,
    val dateCreated: LocalDateTime,
    var agreedVisitor: Boolean,
    var agreedOwner: Boolean,
    val concluded: Boolean,
    val owner: Boolean
)
