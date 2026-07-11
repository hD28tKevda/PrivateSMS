package com.example.smscrypto

sealed class ChatItem {
    data class MessageItem(val message: SmsUtils.Message) : ChatItem()
    data class DateSeparator(val dateText: String) : ChatItem()
}