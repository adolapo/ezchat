package com.example.ezchat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

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

        // Initialize chat participants' data
        currentUserId = intent.getStringExtra(IntentConstants.USER_ID_EXTRA)
        chatParticipantId = intent.getStringExtra(IntentConstants.CHAT_PARTICIPANT_ID_EXTRA)
        chatParticipantUsername = intent.getStringExtra(IntentConstants.CHAT_PARTICIPANT_USER_NAME_EXTRA) // TODO: Change to username
        chatRoomID = intent.getStringExtra(IntentConstants.CHAT_ROOM_ID_EXTRA)
        currentUsername = intent.getStringExtra(IntentConstants.USER_NAME_EXTRA)
        mapUsernames = mapOf(currentUserId to currentUsername, chatParticipantId to chatParticipantUsername)

        Log.e(TAG, "MESSAGES_CHILD: " + DatabaseConstants.MESSAGES_NODE)
        dbReference = FirebaseDatabase.getInstance().reference
        dbReferenceMessages = dbReference.child(DatabaseConstants.MESSAGES_NODE).child(chatRoomID!!)
        Log.e(TAG, "Chat room ID: $chatRoomID")

        chatRoomLayoutManager = LinearLayoutManager(this)
        chatRoomHeader = findViewById<TextView>(R.id.chatHeaderLabel).apply{
            text = chatParticipantUsername
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
            mChatMessageSender?.text = mapUsernames?.get(data.senderId)
            mTimestamp = data.timeStamp
            imageUrl = data.imageUrl
        }
    }

    fun sendMessageOnClick(v: View){
        val messageText = messageEditText.text.toString()
        if (messageText.isNotEmpty()){
            // TODO: Check to make sure both user Ids and usernames are not null

            // Send message
            val message = ChatRoomMessage(currentUserId!!, "", messageText, System.currentTimeMillis())
            dbReference.child(DatabaseConstants.MESSAGES_NODE)
                .child(chatRoomID!!)
                .push()
                .setValue(message)
            messageEditText.setText("")

            // Update chatHeads for both users
            Log.e(TAG, "RoomID: $chatRoomID")
            val chatHead = ChatHead(mapOf(currentUserId to currentUsername, chatParticipantId to chatParticipantUsername),
                                              message)
            dbReference.child(DatabaseConstants.CHAT_ROOMS_NODE)
                .child(currentUserId!!)
                .child(chatRoomID!!)
                .setValue(chatHead)
            dbReference.child(DatabaseConstants.CHAT_ROOMS_NODE)
                .child(chatParticipantId!!)
                .child(chatRoomID!!)
                .setValue(chatHead)
        }
    }

    companion object {
        private const val TAG = "ChatRoomActivity"

        private var currentUserId: String? = null
        private var currentUsername: String? = null
        private var chatParticipantId: String? = null
        private var chatParticipantUsername: String? = null
        private var chatRoomID: String? = null
        private var mapUsernames: Map<String?, String?>? = emptyMap();
    }
}