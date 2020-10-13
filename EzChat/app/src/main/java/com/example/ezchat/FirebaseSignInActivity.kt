package com.example.ezchat

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class FirebaseSignInActivity : AppCompatActivity() {
    //Firebase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firebase_sign_in)

    }

    fun login(view: View){
        //Check if user is already signed in
        val user = Firebase.auth.currentUser
        if (user != null){
            print(TAG + "User already logged in")

            // Start list of recent chat activities
            val inboxActivityIntent = Intent(this, InboxActivity::class.java)
                .putExtra(USER_ID_TAG, user.uid)
                .putExtra(USER_NAME_EXTRA, user.displayName) //TODO: Fix this. Fetch Username from DB instead
            startActivity(inboxActivityIntent)
        } else {
            createSignInIntent()
        }
    }

    fun signOut(view: View){
        FirebaseAuth.getInstance().signOut()
        print("Successfully signed user out")
    }

    private fun createSignInIntent(){
        // Authentication Providers
        val signInProviders = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        // Create and launch sign-in Intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(signInProviders)
                .build(),
            RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN){
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK){
                // Sign in successful
                val user = FirebaseAuth.getInstance().currentUser
                print("Logged in User: " + user?.uid)

                // Start list of recent chat activities
                val inboxActivityIntent = Intent(this, InboxActivity::class.java)
                    .putExtra(USER_ID_TAG, user?.uid)
                startActivity(inboxActivityIntent)
            } else {
                Log.e(TAG, response?.error.toString())
            }
        }
    }

    companion object{
        private const val RC_SIGN_IN = 123
        private const val TAG = "Sign-In-Activity"
        private const val USER_ID_TAG = "currentUserId"
        private const val USER_NAME_EXTRA = "currentUserName"
    }
}