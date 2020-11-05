package com.example.ezchat
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*

class InboxActivity : AppCompatActivity() {
    // User Data
    private var currentUserId: String? = null
    private var currentUserName: String? = null

    // Layout related variables
    private lateinit var chatHeadsRecyclerView: RecyclerView
    private lateinit var chatHeadsLayoutManager: RecyclerView.LayoutManager
    private lateinit var newChatFloatingButton: FloatingActionButton;

    // View Model
    private lateinit var viewModel: InboxActivityViewModel;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

        // Get intent extras
        currentUserId = intent.getStringExtra(IntentConstants.USER_ID_EXTRA)
        currentUserName = intent.getStringExtra(IntentConstants.USER_NAME_EXTRA)

        // Initialize view model and layout vars
        viewModel = InboxActivityViewModel(currentUserId!!, currentUserName!!, this)

        newChatFloatingButton = findViewById<FloatingActionButton>(R.id.newChatButton).apply {
            setOnClickListener { viewModel.startActivityToFindUser(this) }
        }

        chatHeadsLayoutManager = LinearLayoutManager(this)
        chatHeadsRecyclerView = findViewById<RecyclerView>(R.id.inbox_recycler_view).apply {
            layoutManager = chatHeadsLayoutManager // Specify recycler view layout manager
            adapter =
                viewModel.chatHeadsFirebaseRecyclerViewAdapter // Set Recycler View Adapter to be firebase adapter
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            viewModel.START_NEW_CHAT_REQUEST_CODE -> {
                if (resultCode == RESULT_OK){
                    viewModel.launchChatRoomActivityForNewChat(data)
                }
            }
            else -> { }
        }
    }
}