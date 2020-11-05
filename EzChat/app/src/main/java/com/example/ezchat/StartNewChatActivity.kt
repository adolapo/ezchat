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
    private lateinit var viewModel: StartNewChatActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_new_chat)

        val currentUserId = intent.getStringExtra(IntentConstants.USER_ID_EXTRA)
        viewModel = StartNewChatActivityViewModel(this, currentUserId!!)

        // Layout Initializations
        usernameSearchButton = findViewById(R.id.usernameSearchButton)
        usernameEditText = findViewById(R.id.searchUsernameEditText)
        usernameEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(charSequnce: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(charSequnce: Editable?) { }

            override fun onTextChanged(charSequnce: CharSequence?, p1: Int, p2: Int, p3: Int) {
                usernameSearchButton.isEnabled = charSequnce.toString().trim().isNotEmpty()
            }
        })
    }

    fun onSearch(view: View){
        Log.e(TAG, "Attempting to search for a new user")
        val username = usernameEditText.text.toString()
        viewModel.searchForUserByUsername(username)
    }

    companion object{
        const val TAG = "StartNewChatActivity"
    }
}