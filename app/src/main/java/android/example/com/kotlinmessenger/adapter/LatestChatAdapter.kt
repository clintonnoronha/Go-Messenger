package android.example.com.kotlinmessenger.adapter

import android.content.Context
import android.content.Intent
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.messages.ChatLogActivity
import android.example.com.kotlinmessenger.model.ChatMessages
import android.example.com.kotlinmessenger.model.User
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.collections.ArrayList

class LatestChatAdapter(val context: Context, private val chatArrayList: ArrayList<ChatMessages>) :
    RecyclerView.Adapter<LatestChatAdapter.LatestChatViewHolder>(){

    companion object {
        val tempList = LinkedHashMap<User, ChatMessages>()
    }

    class LatestChatViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val imgContactRecentMessages: CircleImageView = view.findViewById(R.id.imgContactRecentMessages)
        val txtContactNameRecentMessages: TextView = view.findViewById(R.id.txtContactNameRecentMessages)
        val txtLatestMessage: TextView = view.findViewById(R.id.txtLatestMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LatestChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.latest_message_single_row, parent, false)
        return LatestChatViewHolder(view)
    }

    override fun getItemCount(): Int {
        return chatArrayList.size
    }

    override fun onBindViewHolder(holder: LatestChatViewHolder, position: Int) {
        var userObject: User ? = null
        val userLatestChat = chatArrayList[position]
        holder.txtLatestMessage.text = userLatestChat.text

        val chatPartnerId: String
        chatPartnerId = if (userLatestChat.fromId == FirebaseAuth.getInstance().uid)
            userLatestChat.toId
        else
            userLatestChat.fromId

        val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                userObject = user
                if (user != null) {
                    tempList[user] = userLatestChat
                    holder.txtContactNameRecentMessages.text = user.username
                    if (user.profileImageUrl.isNotEmpty()) {
                        Glide.with(context).load(user.profileImageUrl)
                            .into(holder.imgContactRecentMessages)
                    } else {
                        holder.imgContactRecentMessages
                            .setImageResource(R.drawable.default_user_image)
                    }
                }
            }
        })

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatLogActivity::class.java)
            intent.putExtra(UserItemAdapter.USER_NAME_KEY, userObject)
            context.startActivity(intent)
        }

    }
}