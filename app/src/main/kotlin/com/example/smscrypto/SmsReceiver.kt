package com.example.smscrypto

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "sms_crypto_channel"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullMessage = messages[0]
        val sender = fullMessage.displayOriginatingAddress ?: return
        val body = fullMessage.displayMessageBody ?: ""

        processFullMessage(context, sender, body)
    }

    private fun processFullMessage(context: Context, from: String, base64Body: String) {
        try {
            val packet = Base64.decode(base64Body.trim(), Base64.DEFAULT)
            val myPrivKey = PrefsManager.getMyPrivateKey(context)
            val decryptedText = if (myPrivKey != null) {
                try {
                    CryptoManager.decryptMessage(packet, myPrivKey)
                } catch (e: Exception) {
                    base64Body
                }
            } else {
                base64Body
            }

            // Помечаем чат как непрочитанный
            PrefsManager.setChatRead(context, from, false)

            showNotification(context, from, decryptedText)
        } catch (e: Exception) {
            PrefsManager.setChatRead(context, from, false)
            showNotification(context, from, base64Body)
        }
    }

    private fun showNotification(context: Context, sender: String, message: String) {
        if (PrefsManager.isContactBlocked(context, sender)) return
        if (PrefsManager.isContactMuted(context, sender)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Encrypted Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for decrypted incoming SMS"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("address", sender)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            sender.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val senderName = SmsUtils.getContactDisplayName(context, sender)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(senderName)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = NOTIFICATION_ID_BASE + sender.hashCode()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}