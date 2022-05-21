package com.example.skucise

import android.os.Build
import androidx.annotation.RequiresApi
import org.json.JSONObject
import java.time.LocalDateTime

data class Message(
    val Id: UInt,
    val SenderId: UInt,
    val ReceiverId: UInt,
    val Content: String,
    val SendDate: LocalDateTime,
    val Seen: Boolean
)

data class UserDisplay(
    val Id: UInt,
    val DisplayName: String,
    val Online: Boolean
)

data class RecentMessage(
    val User: UserDisplay,
    val MessageOrMeeting: MessageOrMeeting
    //val MessageOrMeeting: MessageOrMeeting
)

data class MessageOrMeeting(
    val Message: Message?,
    val Meeting: Meeting?,
    val IsMessage: Boolean
)

class MessageJSON {
    companion object {
        public fun toUserDisplay(user: JSONObject): UserDisplay{
            return UserDisplay(
                user.getInt("id").toUInt(),
                user.getString("displayName"),
                user.getBoolean("online")
            )
        }

        @RequiresApi(Build.VERSION_CODES.O)
        public fun toMessage(msg: JSONObject): Message{
            return Message(
                msg.getInt("id").toUInt(),
                msg.getInt("senderId").toUInt(),
                msg.getInt("receiverId").toUInt(),
                msg.getString("content"),
                LocalDateTime.parse(msg.getString("sendDate")),
                msg.getBoolean("seen")
            )
        }

        @RequiresApi(Build.VERSION_CODES.O)
        public fun toMeeting(meet: JSONObject): Meeting{
            return Meeting(
                id = meet.getInt("id"),
                advertId = meet.getInt("advertId"),
                otherUser = meet.getInt("otherUser"),
                username = meet.getString("username"),
                title = meet.getString("title"),
                proposedTime = LocalDateTime.parse(meet.getString("proposedTime")),
                dateCreated = LocalDateTime.parse(meet.getString("dateCreated")),
                agreedVisitor = meet.getBoolean("agreedVisitor"),
                agreedOwner = meet.getBoolean("agreedOwner"),
                concluded = meet.getBoolean("concluded"),
                owner = meet.getBoolean("owner"),
            )
        }

        @RequiresApi(Build.VERSION_CODES.O)
        public fun toMessageOrMeeting(msgOrMeet: JSONObject): MessageOrMeeting{
            val isMessage = msgOrMeet.getBoolean("isMessage")
            return MessageOrMeeting(
                IsMessage = isMessage,
                Message = (if (isMessage) toMessage(msgOrMeet.getJSONObject("message")) else null),
                Meeting =  (if (isMessage) null else toMeeting(msgOrMeet.getJSONObject("meeting"))),
            )
        }

        @RequiresApi(Build.VERSION_CODES.O)
        public fun toRecentMessage(jsonObj: JSONObject): RecentMessage{
            val user = jsonObj.getJSONObject("user")
            val msg = jsonObj.getJSONObject("message")
            return RecentMessage(
                toUserDisplay(user),
                toMessageOrMeeting(msg)
            )
        }
    }
}