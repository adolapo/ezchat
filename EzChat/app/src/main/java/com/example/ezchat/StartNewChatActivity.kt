package com.example.ezchat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.database.*

class StartNewChatActivity : AppCompatActivity() {
    private lateinit var usernameEditText: EditText
    private lateinit var usernameSearchButton: Button
    private lateinit var dbReference: DatabaseReference
    private lateinit var dbReferenceUsers: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_new_chat)

        // Get intent extras
        currentUserId = intent.getStringExtra(USER_ID_EXTRA)

        // Database initializations
        dbReference = FirebaseDatabase.getInstance().reference
        dbReferenceUsers = dbReference.child(USERS_NODE)

        // View Initializations
        usernameSearchButton = findViewById(R.id.usernameSearchButton)
        usernameEditText = findViewById(R.id.searchUsernameEditText)
        usernameEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(charSequnce: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(charSequnce: CharSequence?, p1: Int, p2: Int, p3: Int) {
                usernameSearchButton.isEnabled = charSequnce.toString().trim().isNotEmpty()
            }

            override fun afterTextChanged(charSequnce: Editable?) {}
        })
    }

    fun onSearch(view: View){
        Log.e(TAG, "Attempting to search for a new user")
        val username = usernameEditText.text.toString()

        // Search for username and build room Id
        val queryListener = object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val resultSet = snapshot.value
                if (resultSet != null){
                    val toastMessage = "Loading chat room with: $username"
                    Toast.makeText(view.context, toastMessage, Toast.LENGTH_LONG).show()

                    // Parse snapshot - result set of format {userId:{firstName, lastName, username...}}
                    val resultSetOuterLayer = resultSet as Map<String, *>
                    Log.e(TAG, "ResultOuterLayer: $resultSetOuterLayer")
                    val userId = resultSetOuterLayer.keys.elementAt(0)
                    Log.e(TAG, "UserId: $userId")
                    val resultSetInnerLayer = resultSetOuterLayer.get(userId) as? Map<String,*>
                    val firstName = resultSetInnerLayer?.get("firstName")
                    val lastName =  resultSetInnerLayer?.get("lastName")
                    val photoUrl = "" //resultSetInnerLayer?.get("photoUrl")
                    Log.e(TAG, "Found user ID: $userId, firstName: $firstName, lastName: $lastName, username: $username")
                    Log.e(TAG, "UserId type: ${userId::class.simpleName}")
                    endActivity(userId, username, photoUrl as String)

                }else{
                    val toastMessage = "Could not find user matching: $username"
                    Toast.makeText(view.context, toastMessage, Toast.LENGTH_LONG).show()
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, error.message)
                Toast.makeText(view.context, "Error", Toast.LENGTH_LONG).show()
            }
        }
        dbReferenceUsers.orderByChild(USERNAME_FIELD_KEY).equalTo(username).addListenerForSingleValueEvent(queryListener)
    }

    fun endActivity(userId: String, username: String, userPhotoUrl: String){

        // Construct chat room Id
        val compareResult = currentUserId!!.compareTo(userId)
        val newChatRoomId = if (compareResult < 0){
            "$currentUserId,$userId"
        } else {
            "$userId,$currentUserId"
        }
        Log.e(TAG, "Found ID: $newChatRoomId, Found name: $username")
        val intent = Intent()
            .putExtra(CHAT_ROOM_ID_EXTRA, newChatRoomId)
            .putExtra(CHAT_PARTICIPANT_ID_EXTRA, userId)
            .putExtra(CHAT_PARTICIPANT_NAME_EXTRA, username)
            .putExtra(CHAT_PARTICIPANT_PHOTO_URL_EXTRA, userPhotoUrl)
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object{
        const val TAG = "StartNewChatActivity"

        private var currentUserId: String? = null

        // Tags for intent extras
        private const val USER_ID_EXTRA = "currentUserId"
        private const val CHAT_ROOM_ID_EXTRA = "roomId"
        private const val CHAT_PARTICIPANT_ID_EXTRA = "participantId"
        private const val CHAT_PARTICIPANT_NAME_EXTRA = "particpantName"
        private const val CHAT_PARTICIPANT_PHOTO_URL_EXTRA = "participantPhotoUrl"

        const val USERS_NODE = "userData"
        const val USERNAME_FIELD_KEY = "username"
    }
}