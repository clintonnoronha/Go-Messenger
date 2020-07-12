package android.example.com.kotlinmessenger.messages

import android.example.com.kotlinmessenger.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class ChatLogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        supportActionBar?.title = "Chat"

    }
}