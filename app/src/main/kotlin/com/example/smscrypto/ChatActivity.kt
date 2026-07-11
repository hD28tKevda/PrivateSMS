package com.example.smscrypto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var tvContactName: TextView
    private lateinit var recyclerMessages: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnMore: ImageButton
    private lateinit var btnEncryptionToggle: ImageButton
    private lateinit var indicatorsContainer: LinearLayout
    private lateinit var indicatorMuted: TextView
    private lateinit var indicatorBlocked: TextView
    private var contactAddress: String = ""
    private lateinit var adapter: MessageAdapter
    private var encryptionEnabled: Boolean = false

    companion object {
        private const val NOTIFICATION_ID_BASE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        contactAddress = intent.getStringExtra("address") ?: ""
        tvContactName = findViewById(R.id.tvContactName)
        recyclerMessages = findViewById(R.id.recyclerMessages)
        etInput = findViewById(R.id.etInput)
        btnSend = findViewById(R.id.btnSend)
        btnMore = findViewById(R.id.btn_more)
        btnEncryptionToggle = findViewById(R.id.btn_encryption_toggle)
        indicatorsContainer = findViewById(R.id.indicators_container)
        indicatorMuted = findViewById(R.id.indicator_muted)
        indicatorBlocked = findViewById(R.id.indicator_blocked)

        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_BASE + contactAddress.hashCode())
        PrefsManager.setChatRead(this, contactAddress, true)

        val pubKey = PrefsManager.getContactPublicKey(this, contactAddress)
        if (pubKey != null && pubKey.isNotEmpty()) {
            encryptionEnabled = true
            PrefsManager.setEncryptionEnabled(this, contactAddress, true)
        } else {
            encryptionEnabled = PrefsManager.isEncryptionEnabled(this, contactAddress)
        }
        updateEncryptionIcon()

        etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank()) {
                    btnSend.setColorFilter(Color.parseColor("#343434"))
                } else {
                    btnSend.setColorFilter(Color.WHITE)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        tvContactName.text = SmsUtils.getContactDisplayName(this, contactAddress)
        tvContactName.setOnClickListener {
            val intent = Intent(this, ContactKeyActivity::class.java).apply {
                putExtra("address", contactAddress)
                putExtra("displayName", tvContactName.text.toString())
            }
            startActivity(intent)
        }

        updateIndicators()

        btnMore.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            val menu = popup.menu
            menu.add(0, 1, 0, "Send my Public Key")

            val muted = PrefsManager.isContactMuted(this, contactAddress)
            menu.add(0, 2, 1, if (muted) "Unmute" else "Mute")

            val blocked = PrefsManager.isContactBlocked(this, contactAddress)
            val blockTitle = if (blocked) "Unblock" else "Block"
            val blockItem = menu.add(0, 3, 2, blockTitle)

            val spannable = SpannableString(blockTitle)
            spannable.setSpan(ForegroundColorSpan(Color.RED), 0, blockTitle.length, 0)
            blockItem.setTitle(spannable)

            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    1 -> {
                        val myPubKey = PrefsManager.getMyPublicKey(this)
                        if (myPubKey.isNullOrEmpty()) {
                            Toast.makeText(this, "No public key saved. Generate it first.", Toast.LENGTH_LONG).show()
                        } else {
                            etInput.setText(myPubKey)
                            etInput.setSelection(myPubKey.length)
                        }
                        true
                    }
                    2 -> {
                        PrefsManager.setContactMuted(this, contactAddress, !muted)
                        Toast.makeText(this, if (!muted) "Chat muted" else "Chat unmuted", Toast.LENGTH_SHORT).show()
                        updateIndicators()
                        true
                    }
                    3 -> {
                        PrefsManager.setContactBlocked(this, contactAddress, !blocked)
                        Toast.makeText(this, if (!blocked) "Contact blocked" else "Contact unblocked", Toast.LENGTH_SHORT).show()
                        updateIndicators()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        btnEncryptionToggle.setOnClickListener {
            encryptionEnabled = !encryptionEnabled
            PrefsManager.setEncryptionEnabled(this, contactAddress, encryptionEnabled)
            updateEncryptionIcon()
        }

        recyclerMessages.layoutManager = LinearLayoutManager(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), 200)
        } else {
            loadMessages()
        }

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            if (encryptionEnabled) {
                val pubKey = PrefsManager.getContactPublicKey(this, contactAddress)
                if (pubKey == null) {
                    Toast.makeText(this, "No public key for this contact. Enter it first.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                try {
                    val encrypted = CryptoManager.encryptMessage(text, pubKey)
                    val base64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
                    SmsSender(this).sendLongSms(contactAddress, base64)
                } catch (e: Exception) {
                    Toast.makeText(this, "Encryption error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                try {
                    SmsSender(this).sendLongSms(contactAddress, text)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error sending SMS: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            etInput.text.clear()
            loadMessages()
        }
    }

    private fun updateEncryptionIcon() {
        if (encryptionEnabled) {
            btnEncryptionToggle.setImageResource(R.drawable.ic_lock)
        } else {
            btnEncryptionToggle.setImageResource(R.drawable.ic_lock_open)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMessages()
        }
    }

    private fun loadMessages() {
        val rawMessages = SmsUtils.getMessages(this, contactAddress)
        val myPrivKey = PrefsManager.getMyPrivateKey(this)
        val decryptedList = rawMessages.map { msg ->
            val displayText = if (myPrivKey != null) {
                try {
                    val packet = Base64.decode(msg.body, Base64.DEFAULT)
                    CryptoManager.decryptMessage(packet, myPrivKey)
                } catch (e: Exception) {
                    msg.body
                }
            } else msg.body
            msg.copy(body = displayText)
        }

        val items = mutableListOf<ChatItem>()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        var lastDate: String? = null

        for (msg in decryptedList) {
            calendar.timeInMillis = msg.date
            val currentDate = dateFormat.format(Date(msg.date))
            if (currentDate != lastDate) {
                items.add(ChatItem.DateSeparator(currentDate))
                lastDate = currentDate
            }
            items.add(ChatItem.MessageItem(msg))
        }

        adapter = MessageAdapter(items)
        recyclerMessages.adapter = adapter
        recyclerMessages.scrollToPosition(adapter.itemCount - 1)
    }

    private fun updateIndicators() {
        val muted = PrefsManager.isContactMuted(this, contactAddress)
        val blocked = PrefsManager.isContactBlocked(this, contactAddress)

        indicatorMuted.visibility = if (muted) View.VISIBLE else View.GONE
        indicatorBlocked.visibility = if (blocked) View.VISIBLE else View.GONE
        indicatorsContainer.visibility = if (muted || blocked) View.VISIBLE else View.GONE
    }
}