package android.example.com.kotlinmessenger.adapter


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.messages.ChatLogActivity
import android.example.com.kotlinmessenger.model.User
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView


class UserItemAdapter(val context: Context, val userArrayList: ArrayList<User>) :
    RecyclerView.Adapter<UserItemAdapter.UserItemViewHolder>() {

    companion object {
        const val USER_NAME_KEY = "USER_NAME_KEY"
        const val INTENT_ACTION_SEND = "INTENT_ACTION_SEND"
        const val INTENT_TEXT_KEY = "INTENT_TEXT_KEY"
    }

    class UserItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val txtContactName: TextView = view.findViewById(R.id.txtContactName)
        val imgContact: CircleImageView = view.findViewById(R.id.imgContact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_single_row, parent,false)
        return UserItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userArrayList.size
    }

    override fun onBindViewHolder(holder: UserItemViewHolder, position: Int) {

        val user = userArrayList[position]
        holder.txtContactName.text = user.username
        val sharedPreferences = context.getSharedPreferences(context.getString(R.string.shared_intent), Context.MODE_PRIVATE)
        val text = sharedPreferences.getString(INTENT_ACTION_SEND, "")
        //Log.i("test2", text!!)
        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(context).load(user.profileImageUrl)
                .into(holder.imgContact)
        } else {
            holder.imgContact.setImageResource(R.drawable.default_user_image)
        }

        //open ChatLogActivity on selecting a user
        holder.itemView.setOnClickListener {

            val intent = Intent(context, ChatLogActivity::class.java)
            intent.putExtra(USER_NAME_KEY, user)
            if (text != "")
                intent.putExtra(INTENT_TEXT_KEY, text)
            context.startActivity(intent)
            (context as Activity).finish()
        }
    }
}