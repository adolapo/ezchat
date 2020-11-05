package com.example.ezchat

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class InboxActivityViewModel( userId: String,
                              username: String,
                              private val parentContext: InboxActivity) : ViewModel() {

    // Firebase related variables
    private val dbReference: DatabaseReference = FirebaseDatabase.getInstance().reference // TODO: Possibly move dbReferences to model
    private var dbReferenceChatRooms: DatabaseReference
    private var chatRoomDataParser: SnapshotParser<ChatHead?>
    private var firebaseRecyclerViewOptions: FirebaseRecyclerOptions<ChatHead>
    var chatHeadsFirebaseRecyclerViewAdapter: FirebaseRecyclerAdapter<ChatHead, ChatHeadViewHolder>
    val START_NEW_CHAT_REQUEST_CODE = 1001

    init {
        currentUserName = username
        currentUserId = userId

        // Set up Database reference and firebase recycler view adapter
        dbReferenceChatRooms =
            dbReference.child(DatabaseConstants.CHAT_ROOMS_NODE).child(currentUserId!!)
        chatRoomDataParser =
            SnapshotParser<ChatHead?> { dataSnapshot ->
                Log.e(TAG, dataSnapshot.value.toString())
                val chatHead = dataSnapshot.getValue(ChatHead::class.java)
                chatHead?.key = dataSnapshot.key
                chatHead!!
            }
        firebaseRecyclerViewOptions = FirebaseRecyclerOptions.Builder<ChatHead>() // TODO: Modify this so it selects sorts chat rooms
            .setQuery(dbReferenceChatRooms.orderByChild(DatabaseConstants.MOST_RECENT_MESSAGE_TIMESTAMP), chatRoomDataParser)
            .setLifecycleOwner(parentContext)
            .build()
        chatHeadsFirebaseRecyclerViewAdapter =
            object : FirebaseRecyclerAdapter<ChatHead, ChatHeadViewHolder>(firebaseRecyclerViewOptions){
                override fun onCreateViewHolder(parent: ViewGroup, type: Int): ChatHeadViewHolder {
                    val newChatHead = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_chat_head, parent, false)
                    return ChatHeadViewHolder(newChatHead)
                }

                override fun onBindViewHolder(holder: ChatHeadViewHolder, position: Int, data: ChatHead){
                    data.setChatParticipantDetails(currentUserId!!)
                    holder.bind(data);
                    Log.e(TAG, "here after bind")
                }

                override fun onDataChanged() {
                    super.onDataChanged()
                    // TODO: Implement
                }

                // Overriding to reverse order of chat room heads
                override fun getItem(position: Int): ChatHead {
                    return super.getItem(itemCount - (position + 1))
                }
            }
    }

    fun startActivityToFindUser(v: View) {
        val intent = Intent(parentContext, StartNewChatActivity::class.java)
            .putExtra(IntentConstants.USER_ID_EXTRA, currentUserId)
        startActivityForResult(parentContext, intent, START_NEW_CHAT_REQUEST_CODE, null)
    }

    fun launchChatRoomActivityForNewChat(data : Intent?){
        val chatRoomActivityIntent = Intent(parentContext, ChatRoomActivity::class.java)
            .putExtra(IntentConstants.USER_ID_EXTRA, currentUserId)
            .putExtra(IntentConstants.USER_NAME_EXTRA, currentUserName)
            .putExtra(IntentConstants.CHAT_ROOM_ID_EXTRA,
                data?.getStringExtra(IntentConstants.CHAT_ROOM_ID_EXTRA))
            .putExtra(IntentConstants.CHAT_PARTICIPANT_ID_EXTRA,
                data?.getStringExtra(IntentConstants.CHAT_PARTICIPANT_ID_EXTRA))
            .putExtra( IntentConstants.CHAT_PARTICIPANT_USER_NAME_EXTRA,
                data?.getStringExtra(IntentConstants.CHAT_PARTICIPANT_USER_NAME_EXTRA))
            .putExtra(IntentConstants.CHAT_PARTICIPANT_PHOTO_URL_EXTRA,
                data?.getStringExtra(IntentConstants.CHAT_PARTICIPANT_PHOTO_URL_EXTRA))
        startActivity(parentContext, chatRoomActivityIntent, null)
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
                launchChatRoomActivityForExistingChat(
                    chatHeadView,
                    mChatRoomKey,
                    mChatParticipantId,
                    mChatParticipantUsername,
                    mParticipantPhotoUrl
                )
            }
        }

        fun bind (data: ChatHead){
            // TODO: Uncomment some commented code and remove others
            mChatHeadName?.text = data.participantUsername
            mMostRecentMessage?.text = data.mostRecentMessage.text
            mChatRoomKey = data.key
            Log.e(TAG, "Key within bind: $mChatRoomKey")
            mChatParticipantId = data.participantId
            mParticipantPhotoUrl = data.participantPhotoUrl //TODO: fix to photo url
            mChatParticipantUsername = data.participantUsername
        }

        private fun launchChatRoomActivityForExistingChat(
            view: View,
            mChatRoomKey: String?,
            mChatParticipantId: String?,
            mChatParticipantUsername: String?,
            mParticipantPhotoUrl: String?,
        ) {
            val chatRoomActivityIntent = Intent(view.context, ChatRoomActivity::class.java)
                .putExtra(IntentConstants.USER_ID_EXTRA, currentUserId)
                .putExtra(IntentConstants.USER_NAME_EXTRA, currentUserName)
                .putExtra(IntentConstants.CHAT_ROOM_ID_EXTRA, mChatRoomKey)
                .putExtra(IntentConstants.CHAT_PARTICIPANT_ID_EXTRA, mChatParticipantId)
                .putExtra(IntentConstants.CHAT_PARTICIPANT_USER_NAME_EXTRA, mChatParticipantUsername)
                .putExtra(IntentConstants.CHAT_PARTICIPANT_PHOTO_URL_EXTRA, mParticipantPhotoUrl)
            startActivity(view.context, chatRoomActivityIntent, null)
        }
    }

    companion object{
        const val TAG = "InboxActivity"
        // User values are here so they can be visible within onBind method of adapter
        lateinit var currentUserId: String;
        lateinit var currentUserName: String;
    }
}
