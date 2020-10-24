package com.example.ezchat

class ChatRoomMessage(val senderId: String,
                      val imageUrl: String,
                      val text: String,
                      val timeStamp:Long){
    var key: String? = null

    // Default Empty Constructor needed for firebase
    constructor(): this("","","",0)
}

class ChatHead(val participants: Map<String?, String?>,
               val mostRecentMessage: ChatRoomMessage){
    var key: String? = null;
    var participantId: String? = null
    var participantUsername: String? = null
    var participantPhotoUrl: String? = null

    fun setChatParticipantDetails(currentUserId: String){
        // participants is of form Map<String, String> where the key are userIds
        val temp = participants.keys
        participantId = if(temp.elementAt(0) != currentUserId) temp.elementAt(0) else temp.elementAt(1)
        participantUsername = participants[participantId]
        participantPhotoUrl = "none" // TODO: Fix
    }

    // Default Constructor needed for firebase
    constructor(): this(emptyMap(), ChatRoomMessage())
}