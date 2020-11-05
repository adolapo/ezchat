package com.example.ezchat

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*

class StartNewChatActivityViewModel(private val parentContext: StartNewChatActivity,
                                    userId: String): ViewModel() {
    private val dbReference: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val currentUserId = userId
    private var dbReferenceUsers: DatabaseReference

    init{
        dbReferenceUsers = dbReference.child(DatabaseConstants.USERS_NODE)
    }

    fun searchForUserByUsername(username: String){
        Log.e(StartNewChatActivity.TAG, "Attempting to search for a new user")

        // Search for username and build room Id
        val queryListener = object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val resultSet = snapshot.value
                if (resultSet != null){
                    val toastMessage = "Loading chat room with: $username"
                    Toast.makeText(parentContext, toastMessage, Toast.LENGTH_SHORT).show()

                    // Parse snapshot - result set of format {userId:{firstName, lastName, username...}}
                    val resultSetOuterLayer = resultSet as Map<String, *>
                    Log.e(TAG, "ResultOuterLayer: $resultSetOuterLayer")
                    val userId = resultSetOuterLayer.keys.elementAt(0)
                    Log.e(TAG, "UserId: $userId")
                    val resultSetInnerLayer = resultSetOuterLayer.get(userId) as? Map<String,*>
                    val firstName = resultSetInnerLayer?.get("firstName")
                    val lastName =  resultSetInnerLayer?.get("lastName")
                    val photoUrl = "" //resultSetInnerLayer?.get("photoUrl")
                    Log.e(StartNewChatActivity.TAG, "Found user ID: $userId, firstName: $firstName, lastName: $lastName, username: $username")
                    Log.e(StartNewChatActivity.TAG, "UserId type: ${userId::class.simpleName}")
                    endActivityAndReturnFoundUser(userId, username, photoUrl as String)

                }else{
                    val toastMessage = "Could not find user matching $username"
                    Toast.makeText(parentContext, toastMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, error.message)
                Toast.makeText(parentContext, "Error", Toast.LENGTH_SHORT).show()
            }
        }
        dbReferenceUsers.orderByChild(DatabaseConstants.USERNAME_CHILD).equalTo(username)
            .addListenerForSingleValueEvent(queryListener)
    }

    fun endActivityAndReturnFoundUser(userId: String, username: String, userPhotoUrl: String){

        // Construct chat room Id
        val compareResult = currentUserId.compareTo(userId)
        val newChatRoomId = if (compareResult < 0){
            "$currentUserId,$userId"
        } else {
            "$userId,$currentUserId"
        }
        Log.e(StartNewChatActivity.TAG, "Found ID: $newChatRoomId, Found name: $username")

        // End Activity and return found user details
        val intent = Intent()
            .putExtra(IntentConstants.CHAT_ROOM_ID_EXTRA, newChatRoomId)
            .putExtra(IntentConstants.CHAT_PARTICIPANT_ID_EXTRA, userId)
            .putExtra(IntentConstants.CHAT_PARTICIPANT_USER_NAME_EXTRA, username)
            .putExtra(IntentConstants.CHAT_PARTICIPANT_PHOTO_URL_EXTRA, userPhotoUrl)
        parentContext.setResult(AppCompatActivity.RESULT_OK, intent)
        parentContext.finish()
    }

    companion object{
        const val TAG = "StartNewChatActivity"
    }
}