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
    private lateinit var viewModel: ChatRoomActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        // Initialize chat participants' data
        val currentUserId = intent.getStringExtra(IntentConstants.USER_ID_EXTRA)
        val chatParticipantId = intent.getStringExtra(IntentConstants.CHAT_PARTICIPANT_ID_EXTRA)
        val chatParticipantUsername = intent.getStringExtra(IntentConstants.CHAT_PARTICIPANT_USER_NAME_EXTRA) // TODO: Change to username
        val chatRoomID = intent.getStringExtra(IntentConstants.CHAT_ROOM_ID_EXTRA)
        val currentUsername = intent.getStringExtra(IntentConstants.USER_NAME_EXTRA)
        viewModel = ChatRoomActivityViewModel(this,
                                                currentUserId!!,
                                                currentUsername!!,
                                                chatParticipantId!!,
                                                chatParticipantUsername!!,
                                                chatRoomID!!)
        chatRoomLayoutManager = LinearLayoutManager(this)
        chatRoomHeader = findViewById<TextView>(R.id.chatHeaderLabel).apply{
            text = chatParticipantUsername
        }
        chatRoomPhoto = findViewById<CircleImageView>(R.id.chatMessageSenderPhoto).apply{
            //TODO: Fix image reference
        }
        sendMessageImageView = findViewById(R.id.sendMessageImageView)
        messageEditText = findViewById(R.id.typeMessageEditTextView)
        chatRoomRecyclerView = findViewById<RecyclerView>(R.id.chatRoomRecyclerView).apply{
            layoutManager = chatRoomLayoutManager
            adapter = viewModel.messagesFirebaseRecyclerAdapter
        }
    }

    fun sendMessageOnClick(v: View){
        val messageText = messageEditText.text.toString()
        if (messageText.isNotEmpty()){
            viewModel.sendMessage(messageText)
            messageEditText.setText("")
        }
    }
}