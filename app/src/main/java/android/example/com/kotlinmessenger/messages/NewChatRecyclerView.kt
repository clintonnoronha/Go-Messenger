package android.example.com.kotlinmessenger.messages

import android.content.Intent
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.model.User
import android.example.com.kotlinmessenger.adapter.UserItemAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder

class NewChatRecyclerView : AppCompatActivity() {

    lateinit var newChatRecycler: RecyclerView
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recycler_view_new_chat)

        newChatRecycler = findViewById(R.id.recyclerNewChat)
        uid = FirebaseAuth.getInstance().uid

        supportActionBar?.title = "Select User"

        fetchUserDetails()
    }

    companion object {
        const val USER_NAME_KEY = "USER_NAME_KEY"
    }

    private fun fetchUserDetails() {
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val adapter = GroupAdapter<ViewHolder>()
                snapshot.children.forEach {
                    val user = it.getValue(User::class.java)
                    if (user != null && uid != user.uid) {
                        adapter.add(UserItemAdapter(user))
                    }
                }
                adapter.setOnItemClickListener { item, view ->
                    val userItem = item as UserItemAdapter

                    val intent = Intent(view.context, ChatLogActivity::class.java)
                    intent.putExtra(USER_NAME_KEY, userItem.user)
                    startActivity(intent)
                    finish()
                }

                newChatRecycler.adapter = adapter
            }
        })
    }

}