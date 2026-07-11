package com.example.smscrypto

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val items: List<ChatItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_MESSAGE_LEFT = 1
        private const val TYPE_MESSAGE_RIGHT = 2
        private const val TYPE_DATE_SEPARATOR = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ChatItem.DateSeparator -> TYPE_DATE_SEPARATOR
            is ChatItem.MessageItem -> {
                val msg = (items[position] as ChatItem.MessageItem).message
                if (msg.type == 1) TYPE_MESSAGE_LEFT else TYPE_MESSAGE_RIGHT
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE_SEPARATOR -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_separator, parent, false)
                DateSeparatorViewHolder(view)
            }
            TYPE_MESSAGE_LEFT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_left, parent, false)
                MessageViewHolder(view)
            }
            TYPE_MESSAGE_RIGHT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_right, parent, false)
                MessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChatItem.DateSeparator -> {
                (holder as DateSeparatorViewHolder).bind(item.dateText)
            }
            is ChatItem.MessageItem -> {
                val msg = item.message
                val messageHolder = holder as MessageViewHolder
                messageHolder.body.text = msg.body
                messageHolder.date.text = formatDate(msg.date)

                val params = messageHolder.contentLayout.layoutParams as FrameLayout.LayoutParams
                params.gravity = if (msg.type == 1) Gravity.START else Gravity.END
                messageHolder.contentLayout.layoutParams = params

                // Долгое нажатие – копирование текста
                messageHolder.itemView.setOnLongClickListener {
                    val clipboard = messageHolder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("message", msg.body)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(messageHolder.itemView.context, "Text copied", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
    }

    override fun getItemCount() = items.size

    // ViewHolder для разделителя даты
    class DateSeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDateSeparator)
        fun bind(dateText: String) {
            tvDate.text = dateText
        }
    }

    // ViewHolder для обычного сообщения
    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contentLayout: LinearLayout = view.findViewById(R.id.content_layout)
        val body: TextView = view.findViewById(R.id.tvBody)
        val date: TextView = view.findViewById(R.id.tvDate)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE HH:mm", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }
}