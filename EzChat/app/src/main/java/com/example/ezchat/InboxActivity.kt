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
import de.hdodenhof.circleimageview.CircleImageView
import java.util.concurrent.atomic.AtomicBoolean

class InboxActivity : AppCompatActivity() {

    private lateinit var chatHeadsRecyclerView: RecyclerView
    private lateinit var chatHeadsRecyclerViewAdapter: RecyclerView.Adapter<*>
    private lateinit var chatHeadsLayoutManager: RecyclerView.LayoutManager

    // Firebase variables
    private lateinit var chatRoomDataParser: SnapshotParser<ChatHeadData?>
    private lateinit var firebaseRecyclerViewOptions: FirebaseRecyclerOptions<ChatHeadData>
    private lateinit var chatHeadsFirebaseRecyclerViewAdapter:
            FirebaseRecyclerAdapter<ChatHeadData, ChatHeadViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)
        currentUserId = intent.getStringExtra(USER_ID_EXTRA)
        currentUserName = intent.getStringExtra(USER_NAME_EXTRA)

        chatHeadsLayoutManager = LinearLayoutManager(this)
        chatHeadsRecyclerViewAdapter = ChatHeadsAdapter(sampleChatHeadsData)

        // Set up Database reference and firebase recycler view adapter
        dbReference = FirebaseDatabase.getInstance().reference
        dbReferenceChatRooms = dbReference.child(CHAT_ROOMS_CHILD).child(currentUserId!!)
        chatRoomDataParser =
            SnapshotParser<ChatHeadData?> { dataSnapshot ->
                Log.e(TAG, dataSnapshot.value.toString())
                val chatHead = dataSnapshot.getValue(ChatHeadData::class.java)
                chatHead?.key = dataSnapshot.key
                chatHead!!
            }
        firebaseRecyclerViewOptions = FirebaseRecyclerOptions.Builder<ChatHeadData>() // TODO: Modify this so it selects sorts chat rooms
            .setQuery(dbReferenceChatRooms.orderByChild(MOST_RECENT_MESSAGE_TIMESTAMP), chatRoomDataParser)
            .setLifecycleOwner(this)
            .build()

        chatHeadsFirebaseRecyclerViewAdapter =
            object : FirebaseRecyclerAdapter<ChatHeadData, ChatHeadViewHolder>(firebaseRecyclerViewOptions){
                override fun onCreateViewHolder(parent: ViewGroup, type: Int): ChatHeadViewHolder{
                    val newChatHead = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_chat_head, parent, false)
                    return ChatHeadViewHolder(newChatHead)
                }

                override fun onBindViewHolder(holder: ChatHeadViewHolder, position: Int, data: ChatHeadData){
                    setChatParticipantDetailsAndBind(holder, data)
                    Log.e(TAG, "here after bind")
                }

                override fun onDataChanged() {
                    super.onDataChanged()
                    // TODO: Implement
                }

                // Overriding to reverse order of chat room heads
                override fun getItem(position: Int): ChatHeadData {
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
        private var mChatParticipantName: String? = null

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
                    mChatParticipantName,
                    mParticipantPhotoUrl)
            }
        }

        fun bind (data: ChatHeadData){
            // TODO: Uncomment some commented code and remove others
            mChatHeadName?.text = data.participantName
            mMostRecentMessage?.text = data.mostRecentMessage.text
            mChatRoomKey = data.key
            Log.e(TAG, "Key within bind: $mChatRoomKey")
            mChatParticipantId = data.participantId
            mParticipantPhotoUrl = data.participantPhotoUrl //TODO: fix to photo url
            mChatParticipantName = data.participantName
        }
    }

    class ChatRoomMessage( val sender: String,
                           val imageUrl: String,
                           val text: String,
                           val timeStamp: String){
        // Default empty constructor needed for firebase
        constructor(): this("","","","")
    }
    class ChatHeadData( var key: String?,
                        val participants: String,
                        val mostRecentMessage: ChatRoomMessage) {
        var participantId: String? = null
        var participantName: String? = null
        var participantPhotoUrl: String? = null

        // Default empty constructor needed for firebase
        constructor(): this("", "",
            ChatRoomMessage("","","",""))

        init{
            //TODO: Implement Init
        }
    }

    fun setChatParticipantDetailsAndBind(holder: ChatHeadViewHolder, data: ChatHeadData){
        val temp = data.participants.split(PARTICIPANTS_DELIMITER)

        // participants is of form "ID1,ID2" so split accordingly to find other participant
        data.participantId = if(temp[0] != currentUserId) temp[0] else temp[1]
        if (data.participantId == null) throw IllegalStateException("Chat room participant ID not yet set")
        val eventListener = object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                // Grab chat participant name and photo
                data.participantName = snapshot.child(USERNAME_CHILD).getValue(String::class.java)
                data.participantPhotoUrl = snapshot.child(USER_PHOTO_CHILD).getValue(String::class.java)
                holder.bind(data)
                Log.e(TAG, "never gets here")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Chat room participant username read failed")
            }
        }
        dbReference.child(USERS_CHILD).child(data.participantId.toString())
            .addListenerForSingleValueEvent(eventListener)
    }

    fun searchUsername (v: View){
        val intent = Intent(this, StartNewChatActivity::class.java)
            .putExtra(USER_ID_EXTRA, currentUserId)
        startActivityForResult(intent, START_NEW_CHAT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            START_NEW_CHAT_REQUEST_CODE -> {
                // Start new chatRoom Activity after user has been found
                val chatRoomActivityIntent = Intent( this, ChatRoomActivity::class.java)
                    .putExtra(USER_ID_EXTRA, currentUserId)
                    .putExtra(USER_NAME_EXTRA, currentUserName)
                    .putExtra(CHAT_ROOM_ID_EXTRA, data?.getStringExtra(CHAT_ROOM_ID_EXTRA))
                    .putExtra(CHAT_PARTICIPANT_ID_EXTRA, data?.getStringExtra(CHAT_PARTICIPANT_ID_EXTRA))
                    .putExtra(CHAT_PARTICIPANT_NAME_EXTRA, data?.getStringExtra(CHAT_PARTICIPANT_NAME_EXTRA))
                    .putExtra(CHAT_PARTICIPANT_PHOTO_URL_EXTRA, data?.getStringExtra(CHAT_PARTICIPANT_PHOTO_URL_EXTRA))
                startActivity( this, chatRoomActivityIntent, null)
            }
            else -> {}
        }

    }

    companion object{
        private const val START_NEW_CHAT_REQUEST_CODE = 1001
        private var currentUserId: String? = null
        private var currentUserName: String? = null

        private lateinit var dbReference: DatabaseReference
        private lateinit var dbReferenceChatRooms: DatabaseReference

        // Database constants
        private const val TAG = "InboxActivity"
        private const val MESSAGES_CHILD = "chatDb/chatRoomMessages"
        private const val CHAT_ROOMS_CHILD = "chatDb/chatRooms"
        private const val MOST_RECENT_MESSAGE_TIMESTAMP = "mostRecentMessage/timestamp"
        private const val USERS_CHILD = "userData/"
        private const val USERNAME_CHILD = "username"
        private const val USER_PHOTO_CHILD = "username"
        private const val PARTICIPANTS_DELIMITER = ","

        // Tags for intent extras
        private const val USER_ID_EXTRA = "currentUserId"
        private const val USER_NAME_EXTRA = "currentUserName"
        private const val CHAT_ROOM_ID_EXTRA = "roomId"
        private const val CHAT_PARTICIPANT_ID_EXTRA = "participantId"
        private const val CHAT_PARTICIPANT_NAME_EXTRA = "particpantName"
        private const val CHAT_PARTICIPANT_PHOTO_URL_EXTRA = "participantPhotoUrl"

        fun launchChatRoomActivity(view: View,
                                   mChatRoomKey: String?,
                                   mChatParticipantId: String?,
                                   mChatParticipantName: String?,
                                   mParticipantPhotoUrl: String?){

            val chatRoomActivityIntent = Intent( view.context, ChatRoomActivity::class.java)
                .putExtra(USER_ID_EXTRA, currentUserId)
                .putExtra(USER_NAME_EXTRA, currentUserName)
                .putExtra(CHAT_ROOM_ID_EXTRA, mChatRoomKey)
                .putExtra(CHAT_PARTICIPANT_ID_EXTRA, mChatParticipantId)
                .putExtra(CHAT_PARTICIPANT_NAME_EXTRA, mChatParticipantName)
                .putExtra(CHAT_PARTICIPANT_PHOTO_URL_EXTRA, mParticipantPhotoUrl)
            startActivity( view.context, chatRoomActivityIntent, null)
        }
    }


    //*************************************************************************************************************
    // *****************Obsolete for this activity - will use fire base adapter instead*****************************
    val sampleChatHeadsData: Array<ChatHeadData> = arrayOf(
        ChatHeadData("key", "Participants",
            ChatRoomMessage("Jane", "", "Hey how are you","09-08-20202")),
        ChatHeadData("key", "Participants",
            ChatRoomMessage("Jane", "", "Hey how are you","09-08-20202")),
        ChatHeadData("key", "Participants",
            ChatRoomMessage("Jane", "", "Hey how are you","09-08-20202")),
        ChatHeadData("key", "Participants",
            ChatRoomMessage("Jane", "", "Hey how are you","09-08-20202")),
        ChatHeadData("key", "Participants",
            ChatRoomMessage("Jane", "", "Hey how are you","09-08-20202"))
    )

    class ChatHeadsAdapter(private val chatHeadsData: Array<ChatHeadData>):
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