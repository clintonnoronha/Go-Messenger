package android.example.com.kotlinmessenger.messages

import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.adapter.ChatReceiverItemAdapter
import android.example.com.kotlinmessenger.adapter.ChatSenderItemAdapter
import android.example.com.kotlinmessenger.model.ChatMessages
import android.example.com.kotlinmessenger.model.User
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import de.hdodenhof.circleimageview.CircleImageView


class ChatLogActivity : AppCompatActivity() {

    lateinit var etTypeMessage: EditText
    lateinit var fabSendMessage: View
    lateinit var imgProfileSender: CircleImageView
    lateinit var txtSenderName: TextView
    lateinit var toolbar: androidx.appcompat.widget.Toolbar
    lateinit var recyclerViewChatLog: RecyclerView
    private val adapter = GroupAdapter<ViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log_test)

        etTypeMessage = findViewById(R.id.etTypeMessage)
        fabSendMessage = findViewById(R.id.fabSendMessage)
        imgProfileSender = findViewById(R.id.imgProfileSender)
        txtSenderName = findViewById(R.id.txtSenderName)
        toolbar = findViewById(R.id.toolbar)
        recyclerViewChatLog = findViewById(R.id.recyclerViewChatLog)
        recyclerViewChatLog.adapter = adapter

        setUpToolbar()

        val user = intent.getParcelableExtra<User>(NewChatRecyclerView.USER_NAME_KEY)

        if (user != null) {
            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(this@ChatLogActivity).load(user.profileImageUrl).into(imgProfileSender)
            } else {
                imgProfileSender.setImageResource(R.drawable.default_user_image)
            }
            txtSenderName.text = user.username
        } else {
            txtSenderName.text = "Chat"
            imgProfileSender.setImageResource(R.drawable.default_user_image)
        }

        if (user != null) {
            listenForMessages(user.uid)
        } else {
            Toast.makeText(
                this@ChatLogActivity,
                "Error!! Please try Again",
                Toast.LENGTH_SHORT
            ).show()
        }


        fabSendMessage.setOnClickListener {
            //send the message
            if (user != null) {
                performSendMessage(user.uid)
            }
        }

    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun listenForMessages(toId: String) {
        val fromId: String = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")
        ref.addChildEventListener(object: ChildEventListener{
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessages = snapshot.getValue(ChatMessages::class.java)
                if (chatMessages != null) {
                    if (chatMessages.fromId == FirebaseAuth.getInstance().uid) {
                        adapter.add(ChatSenderItemAdapter(chatMessages.text))
                    } else {
                        adapter.add(ChatReceiverItemAdapter(chatMessages.text))
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

        })
    }

    private fun performSendMessage(toId: String) {
        val fromId = FirebaseAuth.getInstance().uid ?: return
        val text = etTypeMessage.text.toString()
        if (text.isBlank()) return

        val fromRef = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toRef = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessages = ChatMessages(fromId, fromRef.key!!, text, System.currentTimeMillis()/1000, toId)

        fromRef.setValue(chatMessages)
            .addOnSuccessListener {
                etTypeMessage.text.clear()
                //scroll to latest sent message
                recyclerViewChatLog.scrollToPosition(adapter.itemCount - 1)
            }
        toRef.setValue(chatMessages)
    }
}