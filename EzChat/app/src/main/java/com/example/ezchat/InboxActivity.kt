package com.example.ezchat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.*

// Third party APIs
import de.hdodenhof.circleimageview.CircleImageView

class InboxActivity : AppCompatActivity() {

    private lateinit var chatHeadsRecyclerView: RecyclerView
    private lateinit var chatHeadsRecyclerViewAdapter: RecyclerView.Adapter<*>
    private lateinit var chatHeadsLayoutManager: RecyclerView.LayoutManager

    // Firebase variables
    private lateinit var chatRoomDataParser: SnapshotParser<ChatHeadDataModel?>
    private lateinit var firebaseRecyclerViewOptions: FirebaseRecyclerOptions<ChatHeadDataModel>
    private lateinit var chatHeadsFirebaseRecyclerViewAdapter:
            FirebaseRecyclerAdapter<ChatHeadDataModel, ChatHeadViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)
        
        currentUserId = intent.getStringExtra(IntentConstants.USER_ID_EXTRA)
        currentUserName = intent.getStringExtra(IntentConstants.USER_NAME_EXTRA)

        chatHeadsLayoutManager = LinearLayoutManager(this)
        chatHeadsRecyclerViewAdapter = ChatHeadsAdapter(sampleChatHeadsData)

        // Set up Database reference and firebase recycler view adapter
        dbReference = FirebaseDatabase.getInstance().reference
        dbReferenceChatRooms = dbReference.child(DatabaseConstants.CHAT_ROOMS_NODE).child(currentUserId!!)
        chatRoomDataParser =
            SnapshotParser<ChatHeadDataModel?> { dataSnapshot ->
                Log.e(TAG, dataSnapshot.value.toString())
                val chatHead = dataSnapshot.getValue(ChatHeadDataModel::class.java)
                chatHead?.key = dataSnapshot.key
                chatHead!!
            }
        firebaseRecyclerViewOptions = FirebaseRecyclerOptions.Builder<ChatHeadDataModel>() // TODO: Modify this so it selects sorts chat rooms
            .setQuery(dbReferenceChatRooms.orderByChild(DatabaseConstants.MOST_RECENT_MESSAGE_TIMESTAMP), chatRoomDataParser)
            .setLifecycleOwner(this)
            .build()

        chatHeadsFirebaseRecyclerViewAdapter =
            object : FirebaseRecyclerAdapter<ChatHeadDataModel, ChatHeadViewHolder>(firebaseRecyclerViewOptions){
                override fun onCreateViewHolder(parent: ViewGroup, type: Int): ChatHeadViewHolder{
                    val newChatHead = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_chat_head, parent, false)
                    return ChatHeadViewHolder(newChatHead)
                }

                override fun onBindViewHolder(holder: ChatHeadViewHolder, position: Int, data: ChatHeadDataModel){
                    data.setChatParticipantDetails(currentUserId!!)
                    holder.bind(data);
                    Log.e(TAG, "here after bind")
                }

                override fun onDataChanged() {
                    super.onDataChanged()
                    // TODO: Implement
                }

                // Overriding to reverse order of chat room heads
                override fun getItem(position: Int): ChatHeadDataModel {
                    return super.getItem(itemCount - (position + 1))
                }
            }

        chatHeadsRecyclerView = findViewById<RecyclerView>(R.id.inbox_recycler_view).apply{
            layoutManager = chatHeadsLayoutManager // Specify recycler view layout manager
            adapter = chatHeadsFirebaseRecyclerViewAdapter // Set Recycler View Adapter to be firebase adapter
            //adapter = chatHeadsRecyclerViewAdapter
        } 
    }

    class ChatHeadViewHolder(private val chatHeadView: View):
        RecyclerView.ViewHolder(chatHeadView) {
        // Layouts
        private var mChatHeadImage: CircleImageView? = null
        private var mChatHeadName: TextView? = null
        private var mMostRecentMessage: TextView? = null

        // Data
        private var mChatRoomKey: String? = null
        private var mChatParticipantId: String? = null
        private var mParticipantPhotoUrl: String? = null
        private var mChatParticipantUsername: String? = null

        init {
            mChatHeadImage = itemView.findViewById(R.id.chatMessageSenderPhoto)
            mChatHeadName = itemView.findViewById(R.id.chatParticipant)
            mMostRecentMessage = itemView.findViewById(R.id.chatHeadMostRecentMessage)

            // TODO: Set on click listener for view here to spawn new activity
            chatHeadView.setOnClickListener{
                // Launch chatRoomActivity
                Log.e(TAG, "Trying to start room: $mChatRoomKey")
                launchChatRoomActivity(
                    chatHeadView,
                    mChatRoomKey,
                    mChatParticipantId,
                    mChatParticipantUsername,
                    mParticipantPhotoUrl)
            }
        }

        fun bind (data: ChatHeadDataModel){
            // TODO: Uncomment some commented code and remove others
            mChatHeadName?.text = data.participantUsername
            mMostRecentMessage?.text = data.mostRecentMessage.text
            mChatRoomKey = data.key
            Log.e(TAG, "Key within bind: $mChatRoomKey")
            mChatParticipantId = data.participantId
            mParticipantPhotoUrl = data.participantPhotoUrl //TODO: fix to photo url
            mChatParticipantUsername = data.participantUsername
        }
    }

    fun searchUsername (v: View){
        val intent = Intent(this, StartNewChatActivity::class.java)
            .putExtra(IntentConstants.USER_ID_EXTRA, currentUserId)
        startActivityForResult(intent, START_NEW_CHAT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            START_NEW_CHAT_REQUEST_CODE -> {
                // Start new chatRoom Activity after user has been found
                val chatRoomActivityIntent = Intent( this, ChatRoomActivity::class.java)
                    .putExtra(IntentConstants.USER_ID_EXTRA, currentUserId)
                    .putExtra(IntentConstants.USER_NAME_EXTRA, currentUserName)
                    .putExtra(IntentConstants.CHAT_ROOM_ID_EXTRA, data?.getStringExtra(IntentConstants.CHAT_ROOM_ID_EXTRA))
                    .putExtra(IntentConstants.CHAT_PARTICIPANT_ID_EXTRA, data?.getStringExtra(IntentConstants.CHAT_PARTICIPANT_ID_EXTRA))
                    .putExtra(IntentConstants.CHAT_PARTICIPANT_USER_NAME_EXTRA, data?.getStringExtra(IntentConstants.CHAT_PARTICIPANT_USER_NAME_EXTRA))
                    .putExtra(IntentConstants.CHAT_PARTICIPANT_PHOTO_URL_EXTRA, data?.getStringExtra(IntentConstants.CHAT_PARTICIPANT_PHOTO_URL_EXTRA))
                startActivity( this, chatRoomActivityIntent, null)
            }
            else -> {}
        }
    }

    companion object{
        private const val TAG = "InboxActivity"

        private const val START_NEW_CHAT_REQUEST_CODE = 1001
        private var currentUserId: String? = null
        private var currentUserName: String? = null

        private lateinit var dbReference: DatabaseReference
        private lateinit var dbReferenceChatRooms: DatabaseReference
        fun launchChatRoomActivity(view: View,
                                   mChatRoomKey: String?,
                                   mChatParticipantId: String?,
                                   mChatParticipantUsername: String?,
                                   mParticipantPhotoUrl: String?){

            val chatRoomActivityIntent = Intent( view.context, ChatRoomActivity::class.java)
                .putExtra(IntentConstants.USER_ID_EXTRA, currentUserId)
                .putExtra(IntentConstants.USER_NAME_EXTRA, currentUserName)
                .putExtra(IntentConstants.CHAT_ROOM_ID_EXTRA, mChatRoomKey)
                .putExtra(IntentConstants.CHAT_PARTICIPANT_ID_EXTRA, mChatParticipantId)
                .putExtra(IntentConstants.CHAT_PARTICIPANT_USER_NAME_EXTRA, mChatParticipantUsername)
                .putExtra(IntentConstants.CHAT_PARTICIPANT_PHOTO_URL_EXTRA, mParticipantPhotoUrl)
            startActivity( view.context, chatRoomActivityIntent, null)
        }
    }


    //*************************************************************************************************************
    // *****************Obsolete for this activity - will use fire base adapter instead*****************************
    val sampleChatHeadsData: Array<ChatHeadDataModel> = arrayOf(
        ChatHeadDataModel(mapOf("Participants" to "The participants"),
            ChatRoomMessage("Jane", "", "Hey how are you",0)),
        ChatHeadDataModel(mapOf("Participants" to "The participants"),
            ChatRoomMessage("Jane", "", "Hey how are you",0)),
        ChatHeadDataModel(mapOf("Participants" to "The participants"),
            ChatRoomMessage("Jane", "", "Hey how are you",0)),
        ChatHeadDataModel( mapOf("Participants" to "The participants"),
            ChatRoomMessage("Jane", "", "Hey how are you",0)),
        ChatHeadDataModel(mapOf("Participants" to "The participants"),
            ChatRoomMessage("Jane", "", "Hey how are you",0))
    )

    class ChatHeadsAdapter(private val chatHeadsData: Array<ChatHeadDataModel>):
        RecyclerView.Adapter<ChatHeadViewHolder>(){

        // Create a new ChatHead view when Layout manager asks
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatHeadViewHolder {
            val newChatHead = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_head, parent, false)

            return ChatHeadViewHolder(newChatHead)
        }

        //Bind data to new chatHead created
        override fun onBindViewHolder(holder: ChatHeadViewHolder, position: Int) {
            holder.bind(chatHeadsData[position])
        }

        override fun getItemCount(): Int = chatHeadsData.size
    }
}