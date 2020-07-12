package android.example.com.kotlinmessenger.adapter


import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.model.User
import com.bumptech.glide.Glide
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.user_single_row.view.*

class UserItemAdapter(val user: User): Item<ViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.user_single_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.txtContactName.text = user.username
        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(viewHolder.root.context).load(user.profileImageUrl)
                .into(viewHolder.itemView.imgContact)
        } else {
            viewHolder.itemView.imgContact.setImageResource(R.drawable.default_user_image)
        }
    }
}