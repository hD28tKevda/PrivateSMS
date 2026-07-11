package com.example.smscrypto

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class EncryptionActivity : AppCompatActivity() {

    private lateinit var radioGroup: RadioGroup
    private lateinit var btnGenerate: Button
    private lateinit var etPublic: EditText
    private lateinit var etPrivate: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_encryption)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        radioGroup = findViewById(R.id.radioGroupKeySize)
        btnGenerate = findViewById(R.id.btnGenerate)
        etPublic = findViewById(R.id.etPublicKey)
        etPrivate = findViewById(R.id.etPrivateKey)

        etPublic.setText(PrefsManager.getMyPublicKey(this) ?: "")
        etPrivate.setText(PrefsManager.getMyPrivateKey(this) ?: "")

        etPublic.setOnLongClickListener {
            copyToClipboard(etPublic.text.toString())
            true
        }
        etPrivate.setOnLongClickListener {
            copyToClipboard(etPrivate.text.toString())
            true
        }

        btnGenerate.setOnClickListener {
            val keySize = if (radioGroup.checkedRadioButtonId == R.id.radio4096) 4096 else 2048
            val pair = CryptoManager.generateKeyPair(keySize)
            etPublic.setText(pair.publicKeyPem)
            etPrivate.setText(pair.privateKeyPem)

            PrefsManager.setMyPublicKey(this, pair.publicKeyPem)
            PrefsManager.setMyPrivateKey(this, pair.privateKeyPem)
            Toast.makeText(this, "Keys generated and saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String) {
        if (text.isNotEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("key", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }
    }
}