package android.example.com.kotlinmessenger.messages

import android.content.Intent
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.settings.AboutAppActivity
import android.example.com.kotlinmessenger.startup.SignInActivity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.firebase.auth.FirebaseAuth

class LatestMessagesActivity : AppCompatActivity() {

    lateinit var fabNewChat: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)

        fabNewChat = findViewById(R.id.fabNewChat)

        fabNewChat.setOnClickListener {
            val intent = Intent(this, NewChatRecyclerView::class.java)
            startActivity(intent)
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.searchUser -> {
                //perform search to find specific user
            }
            R.id.my_profile -> {
                //display user profile
            }
            R.id.sign_out -> {
                //sign user out
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            R.id.about_app -> {
                //Display App Info
                val intent = Intent(this, AboutAppActivity::class.java)
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

}