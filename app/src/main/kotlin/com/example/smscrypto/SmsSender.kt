package com.example.smscrypto

import android.content.Context
import android.telephony.SmsManager

class SmsSender(private val context: Context) {
    fun sendLongSms(phone: String, text: String) {
        val sms = if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else SmsManager.getDefault()
        val parts = sms.divideMessage(text)
        sms.sendMultipartTextMessage(phone, null, parts, null, null)
    }
}