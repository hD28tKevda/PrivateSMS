package com.example.smscrypto

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

object SmsUtils {
    data class Chat(
        val address: String,
        val lastMessage: String,
        val date: Long,
        var isMuted: Boolean = false,
        var isBlocked: Boolean = false,
        var hasUnread: Boolean = false
    )
    data class Message(val address: String, val body: String, val date: Long, val type: Int)

    fun getChats(context: Context): List<Chat> {
        val uri = Uri.parse("content://sms")
        val projection = arrayOf("address", "body", "date")
        val cursor = context.contentResolver.query(uri, projection, null, null, "date DESC") ?: return emptyList()
        val chatMap = linkedMapOf<String, Chat>()
        while (cursor.moveToNext()) {
            val address = cursor.getString(0) ?: continue
            val body = cursor.getString(1) ?: ""
            val date = cursor.getLong(2)

            if (!chatMap.containsKey(address)) {
                val hasUnread = !PrefsManager.isChatRead(context, address)
                chatMap[address] = Chat(address, body, date, hasUnread = hasUnread)
            }
        }
        cursor.close()
        return chatMap.values.toList()
    }

    fun getMessages(context: Context, address: String): List<Message> {
        val uri = Uri.parse("content://sms")
        val selection = "address = ?"
        val args = arrayOf(address)
        val cursor = context.contentResolver.query(uri, null, selection, args, "date ASC") ?: return emptyList()
        val messages = mutableListOf<Message>()
        while (cursor.moveToNext()) {
            val body = cursor.getString(cursor.getColumnIndexOrThrow("body"))
            val date = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
            val type = cursor.getInt(cursor.getColumnIndexOrThrow("type"))
            messages.add(Message(address, body, date, type))
        }
        cursor.close()
        return messages
    }

    fun getContactDisplayName(context: Context, phone: String): String {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val uri = Uri.withAppendedPath(
                    android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phone)
                )
                val cursor = context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
                    null, null, null
                )
                if (cursor != null) {
                    val name = if (cursor.moveToFirst()) cursor.getString(0) ?: phone else phone
                    cursor.close()
                    return name
                }
            } catch (_: Exception) {}
        }
        return phone
    }

    fun formatChatDate(millis: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = millis }

        return if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
            && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        ) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
        } else {
            val diffDays = (now.timeInMillis - millis) / (1000 * 60 * 60 * 24)
            if (diffDays in 1..7) {
                SimpleDateFormat("EEE", Locale.ENGLISH).format(Date(millis))
            } else {
                SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(millis))
            }
        }
    }
}