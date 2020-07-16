package android.example.com.kotlinmessenger.adapter

import android.example.com.kotlinmessenger.R
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.chat_sender_single_row.view.*

class ChatSenderItemAdapter(val text: String): Item<ViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.chat_sender_single_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.txtSenderMessage.text = text
    }
}