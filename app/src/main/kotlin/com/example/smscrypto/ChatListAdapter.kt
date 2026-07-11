package com.example.smscrypto

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatListAdapter(
    private var chats: List<SmsUtils.Chat>,
    private val onItemClick: (SmsUtils.Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.iv_avatar)
        val name: TextView = view.findViewById(R.id.tv_name)
        val lastMessage: TextView = view.findViewById(R.id.tv_last_message)
        val date: TextView = view.findViewById(R.id.tv_date)
        val indicatorsContainer: LinearLayout = view.findViewById(R.id.indicators_container)
        val indicatorMuted: TextView = view.findViewById(R.id.indicator_muted)
        val indicatorBlocked: TextView = view.findViewById(R.id.indicator_blocked)
        val unreadIndicator: View = view.findViewById(R.id.unread_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]
        val context = holder.itemView.context
        holder.name.text = SmsUtils.getContactDisplayName(context, chat.address)
        holder.lastMessage.text = chat.lastMessage
        holder.date.text = SmsUtils.formatChatDate(chat.date)

        val primaryColor = if (chat.hasUnread) android.graphics.Color.WHITE else context.resources.getColor(R.color.text_primary)
        val secondaryColor = if (chat.hasUnread) android.graphics.Color.WHITE else context.resources.getColor(R.color.text_secondary)

        holder.name.setTextColor(primaryColor)
        holder.lastMessage.setTextColor(secondaryColor)
        holder.date.setTextColor(secondaryColor)

        val style = if (chat.hasUnread) Typeface.BOLD else Typeface.NORMAL
        holder.name.setTypeface(null, style)
        holder.lastMessage.setTypeface(null, style)
        holder.date.setTypeface(null, style)

        holder.unreadIndicator.visibility = if (chat.hasUnread) View.VISIBLE else View.GONE

        val muted = chat.isMuted
        val blocked = chat.isBlocked
        holder.indicatorMuted.visibility = if (muted) View.VISIBLE else View.GONE
        holder.indicatorBlocked.visibility = if (blocked) View.VISIBLE else View.GONE
        holder.indicatorsContainer.visibility = if (muted || blocked) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onItemClick(chat) }
    }

    override fun getItemCount() = chats.size

    fun updateList(newChats: List<SmsUtils.Chat>) {
        chats = newChats
        notifyDataSetChanged()
    }
}