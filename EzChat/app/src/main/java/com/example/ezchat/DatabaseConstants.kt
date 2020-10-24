package com.example.ezchat

object DatabaseConstants {
    // Messages
    const val MESSAGES_NODE = "chatDb/chatRoomMessages";
    const val MOST_RECENT_MESSAGE_TIMESTAMP = "mostRecentMessage/timeStamp"


    //ChatRooms
    const val CHAT_ROOMS_NODE = "chatDb/chatRooms"

    //Users
    const val USERS_NODE = "userData/"
    const val USERNAME_CHILD = "username"
    const val USER_PHOTO_CHILD = "username"

    //Delimiters
    const val PARTICIPANTS_DELIMITER = ","
}