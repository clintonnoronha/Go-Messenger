package android.example.com.kotlinmessenger.messages

import android.content.Context
import android.content.SharedPreferences
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.adapter.ChatReceiverItemAdapter
import android.example.com.kotlinmessenger.adapter.ChatSenderItemAdapter
import android.example.com.kotlinmessenger.adapter.UserItemAdapter
import android.example.com.kotlinmessenger.model.ChatMessages
import android.example.com.kotlinmessenger.model.User
import android.os.Bundle
import android.text.Editable
import android.util.Log
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
    lateinit var sharedPreferences: SharedPreferences
    private val adapter = GroupAdapter<ViewHolder>()
    private var intentText: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        etTypeMessage = findViewById(R.id.etTypeMessage)
        fabSendMessage = findViewById(R.id.fabSendMessage)
        imgProfileSender = findViewById(R.id.imgProfileSender)
        txtSenderName = findViewById(R.id.txtSenderName)
        toolbar = findViewById(R.id.toolbar)
        sharedPreferences = getSharedPreferences(getString(R.string.shared_intent), Context.MODE_PRIVATE)
        recyclerViewChatLog = findViewById(R.id.recyclerViewChatLog)
        recyclerViewChatLog.adapter = adapter

        setUpToolbar()

        val user = intent.getParcelableExtra<User>(UserItemAdapter.USER_NAME_KEY)
        intentText = intent.getStringExtra(UserItemAdapter.INTENT_TEXT_KEY)

        //if data(text) from another app is being shared, assign it to etTypeMessage
        if (!intentText.isNullOrEmpty()) {
            etTypeMessage.text = Editable.Factory.getInstance().newEditable(intentText)
            //Log.i("test1", intentText!!)
            sharedPreferences.edit().remove(UserItemAdapter.INTENT_ACTION_SEND).apply()
        }

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
                //after message is sent clear text in etTypeMessage
                etTypeMessage.text.clear()
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
            override fun onChildRemoved(snapshot: DataSnapshot) {

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
                recyclerViewChatLog.scrollToPosition(adapter.itemCount - 1)
            }
        })
    }

    private fun performSendMessage(toId: String) {
        val fromId = FirebaseAuth.getInstance().uid ?: return
        val text = etTypeMessage.text.toString().trim()
        //Prevent blank messages from being sent
        if (text.isBlank()) return

        val fromRef = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toRef = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessages = ChatMessages(fromId, fromRef.key!!, text, System.currentTimeMillis()/1000, toId)

        fromRef.setValue(chatMessages)
            .addOnSuccessListener {
                //scroll to latest message sent
                recyclerViewChatLog.scrollToPosition(adapter.itemCount - 1)
            }
        toRef.setValue(chatMessages)

        val latestMessageFromRef = FirebaseDatabase.getInstance().getReference(
            "/latest-message/$fromId/$toId"
        )
        latestMessageFromRef.setValue(chatMessages)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference(
            "/latest-message/$toId/$fromId"
        )
        latestMessageToRef.setValue(chatMessages)
    }

}