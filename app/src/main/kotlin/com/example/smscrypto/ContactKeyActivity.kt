package com.example.smscrypto

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ContactKeyActivity : AppCompatActivity() {

    private lateinit var tvPublicKey: TextView
    private var phone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_key)

        // Кнопка «Назад»
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        phone = intent.getStringExtra("address") ?: ""
        val contactName = intent.getStringExtra("displayName") ?: phone

        findViewById<TextView>(R.id.tv_contact_name).text = contactName
        findViewById<TextView>(R.id.tv_contact_phone).text = phone

        tvPublicKey = findViewById(R.id.tvPublicKey)
        tvPublicKey.movementMethod = ScrollingMovementMethod()
        tvPublicKey.text = PrefsManager.getContactPublicKey(this, phone) ?: ""

        // Кнопка очистки
        findViewById<ImageButton>(R.id.btn_clear_key).setOnClickListener {
            tvPublicKey.text = ""
            PrefsManager.setContactPublicKey(this, phone, null)
            Toast.makeText(this, "Public key removed", Toast.LENGTH_SHORT).show()
        }

        // Долгое нажатие – копировать ключ
        tvPublicKey.setOnLongClickListener {
            val text = tvPublicKey.text.toString()
            if (text.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("public_key", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Public key copied", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // Двойной тап – вставить из буфера и сразу сохранить
        tvPublicKey.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val now = System.currentTimeMillis()
                val last = tvPublicKey.tag as? Long ?: 0L
                tvPublicKey.tag = now
                if (now - last < 300) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val pastedText = clip.getItemAt(0).text.toString()
                        tvPublicKey.text = pastedText
                        PrefsManager.setContactPublicKey(this, phone, pastedText)
                        Toast.makeText(this, "Public key saved", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            false
        }
    }
}