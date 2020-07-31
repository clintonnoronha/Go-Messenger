package android.example.com.kotlinmessenger.adapter

import android.content.Context
import android.example.com.kotlinmessenger.R
import android.view.View
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.chat_sender_single_row.view.*

class ChatSenderItemAdapter(
    val context: Context,
    val text: String = "",
    val fileUrl: String = "",
    val mediaUrl: String = "",
    val fileName: String = ""
): Item<ViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.chat_sender_single_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {

        if (text != "") {
            viewHolder.itemView.txtSenderMessage.visibility = View.VISIBLE
            viewHolder.itemView.rlMedia.visibility = View.GONE
            viewHolder.itemView.rlFiles.visibility = View.GONE
            viewHolder.itemView.txtSenderMessage.text = text
        } else if (fileUrl != "") {
            viewHolder.itemView.txtSenderMessage.visibility = View.GONE
            viewHolder.itemView.rlMedia.visibility = View.GONE
            viewHolder.itemView.rlFiles.visibility = View.VISIBLE
            viewHolder.itemView.txtFileName.text = fileName


        } else if (mediaUrl != "") {
            viewHolder.itemView.txtSenderMessage.visibility = View.GONE
            viewHolder.itemView.rlMedia.visibility = View.VISIBLE
            viewHolder.itemView.rlFiles.visibility = View.GONE
            viewHolder.itemView.txtMediaName.text = fileName
        }
    }
}