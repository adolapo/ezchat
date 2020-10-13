package com.example.ezchat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import de.hdodenhof.circleimageview.CircleImageView
import java.sql.Timestamp

class ChatRoomActivity : AppCompatActivity() {
    private lateinit var chatRoomHeader: TextView
    private lateinit var chatRoomPhoto: CircleImageView
    private lateinit var messageEditText: EditText
    private lateinit var sendMessageImageView: ImageView
    private lateinit var chatRoomRecyclerView: RecyclerView
    private lateinit var chatRoomLayoutManager: RecyclerView.LayoutManager

    //Firebase variables
    private lateinit var dbReference: DatabaseReference
    private lateinit var dbReferenceMessages: DatabaseReference
    private lateinit var messageDataParser: SnapshotParser<ChatRoomMessage?>
    private lateinit var firebaseRecyclerViewOptions: FirebaseRecyclerOptions<ChatRoomMessage>
    private lateinit var messagesFirebaseRecyclerAdapter:
            FirebaseRecyclerAdapter<ChatRoomMessage, ChatRoomMessageViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        currentUserId = intent.getStringExtra(USER_ID_EXTRA)
        chatParticipantId = intent.getStringExtra(CHAT_PARTICIPANT_ID_EXTRA)
        chatParticipantName = intent.getStringExtra(CHAT_PARTICIPANT_NAME_EXTRA)
        chatRoomID = intent.getStringExtra(CHAT_ROOM_ID_EXTRA)
        currentUsername = intent.getStringExtra(USER_NAME_EXTRA)

        Log.e(TAG, "MESSAGES_CHILD: $MESSAGES_CHILD")
        dbReference = FirebaseDatabase.getInstance().reference
        dbReferenceMessages = dbReference.child(MESSAGES_CHILD).child(chatRoomID!!)
        Log.e(TAG, "Chat room ID: $chatRoomID")

        chatRoomLayoutManager = LinearLayoutManager(this)
        chatRoomHeader = findViewById<TextView>(R.id.chatHeaderLabel).apply{
            text = chatParticipantName
        }
        chatRoomPhoto = findViewById<CircleImageView>(R.id.chatMessageSenderPhoto).apply{
            //TODO: Fix image reference
        }
        sendMessageImageView = findViewById(R.id.sendMessageImageView)
        messageEditText = findViewById(R.id.typeMessageEditTextView)

        messageDataParser =
            SnapshotParser<ChatRoomMessage?> { dataSnapshot ->
                val message = dataSnapshot.getValue(ChatRoomMessage::class.java)
                Log.e(TAG, message.toString())
                // Store chat head key
                message?.key = dataSnapshot.key
                message!!
            }
        firebaseRecyclerViewOptions = FirebaseRecyclerOptions.Builder<ChatRoomMessage>()
            .setQuery(dbReferenceMessages, messageDataParser)
            .setLifecycleOwner(this)
            .build()
        messagesFirebaseRecyclerAdapter =
            object : FirebaseRecyclerAdapter<ChatRoomMessage, ChatRoomMessageViewHolder>(firebaseRecyclerViewOptions){
                override fun onCreateViewHolder(parent: ViewGroup, type: Int): ChatRoomMessageViewHolder {
                    val newMessage = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_chat_room_message, parent, false)
                    return ChatRoomMessageViewHolder(newMessage)
                }

                override fun onBindViewHolder(holder: ChatRoomMessageViewHolder, position: Int, data: ChatRoomMessage){
                    holder.bind(data)
                }

                override fun onDataChanged() {
                    // There's nothing extra that needs doing
                    super.onDataChanged()
                }
            }
        chatRoomRecyclerView = findViewById<RecyclerView>(R.id.chatRoomRecyclerView).apply{
            layoutManager = chatRoomLayoutManager
            adapter = messagesFirebaseRecyclerAdapter
        }
    }

    class ChatRoomMessageViewHolder(private val chatRoomMessageViewHolder: View):
        RecyclerView.ViewHolder(chatRoomMessageViewHolder){
        // Layouts
        private var mChatMessageText: TextView? = null
        private var mChatMessageSender: TextView? = null

        //Data
        private var mTimestamp: Long? = null
        private var imageUrl: String? = null

        init{
            mChatMessageText = itemView.findViewById(R.id.chatMessageText)
            mChatMessageSender = itemView.findViewById(R.id.chatMessageSender)

            // Set on click listener if need be
        }

        fun bind(data: ChatRoomMessage){
            mChatMessageText?.text = data.text
            mChatMessageSender?.text = data.sender
            mTimestamp = data.timestamp
            imageUrl = data.imageUrl
        }
    }

    class ChatRoomMessage(val sender: String,
                          val text: String,
                          val imageUrl: String,
                          val timestamp: Long){
        var key: String? = null

        // Empty constructor needed for firebase
        constructor(): this("", "", "", 0) //TODO: Fix tomestamp val
    }

    class ChatHeadData( var key: String?,
                        val participants: String,
                        val mostRecentMessage: ChatRoomMessage
    ) {
        private var participantId: String? = null
        var participantName: String? = null
        var participantPhotoUrl: String? = null

        // Default empty constructor needed for firebase
        constructor() : this(
            "", "",
            ChatRoomMessage("", "", "", 0)
        )

        init {
            //TODO: Implement Init
        }
    }

    fun sendMessageOnClick(v: View){
        val messageText = messageEditText.text.toString()
        if (messageText.isNotEmpty()){
            // Send message
            val message = ChatRoomMessage(currentUsername!!, messageText, "", System.currentTimeMillis())
            dbReference.child(MESSAGES_CHILD)
                .child(chatRoomID!!)
                .push()
                .setValue(message)
            messageEditText.setText("")

            // Update chatHeads for both users
            Log.e(TAG, "RoomID: $chatRoomID")
            val chatHead = mapOf("participants" to chatRoomID, "mostRecentMessage" to message) //TODO: FIx chatRoomID should be participants
            dbReference.child(CHAT_ROOMS_CHILD)
                .child(currentUserId!!)
                .child(chatRoomID!!)
                .setValue(chatHead)
            dbReference.child(CHAT_ROOMS_CHILD)
                .child(chatParticipantId!!)
                .child(chatRoomID!!)
                .setValue(chatHead)
        }
    }

    companion object{
        private var currentUserId: String? = null
        private var currentUsername: String? = null
        private var chatParticipantId: String? = null
        private var chatParticipantName: String? = null
        private var chatRoomID: String? = null

        private const val TAG = "ChatRoomActivity"
        private const val USER_ID_EXTRA = "currentUserId"
        private const val CHAT_ROOM_ID_EXTRA = "roomId"
        private const val CHAT_PARTICIPANT_ID_EXTRA = "participantId"
        private const val CHAT_PARTICIPANT_NAME_EXTRA = "particpantName"
        private const val CHAT_PARTICIPANT_PHOTO_URL_EXTRA = "participantPhotoUrl"
        private const val USER_NAME_EXTRA = "currentUserName"

        private var MESSAGES_CHILD = "chatDb/chatRoomMessages"
        private var CHAT_ROOMS_CHILD = "chatDb/chatRooms"
    }
}