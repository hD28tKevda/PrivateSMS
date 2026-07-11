package com.example.smscrypto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*
import kotlin.collections.HashMap

class ChatsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnClear: ImageButton
    private var allChats: List<SmsUtils.Chat> = emptyList()
    private var adapter: ChatListAdapter? = null

    private val contactNameCache = HashMap<String, String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)
        recyclerView = view.findViewById(R.id.recycler_chats)
        etSearch = view.findViewById(R.id.et_search)
        btnClear = view.findViewById(R.id.btn_clear_search)
        recyclerView.layoutManager = LinearLayoutManager(context)

        view.findViewById<ImageView>(R.id.btn_encryption).setOnClickListener {
            startActivity(Intent(activity, EncryptionActivity::class.java))
        }

        view.findViewById<FloatingActionButton>(R.id.fab_new_chat).setOnClickListener {
            startActivity(Intent(activity, ContactsListActivity::class.java))
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                btnClear.visibility = if (query.isBlank()) View.GONE else View.VISIBLE
                filterChats(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClear.setOnClickListener { etSearch.text.clear() }
        return view
    }

    override fun onResume() {
        super.onResume()
        loadChats()
    }

    private fun loadChats() {
        val ctx = requireContext()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(ctx, "SMS permission required", Toast.LENGTH_SHORT).show()
            return
        }

        allChats = SmsUtils.getChats(ctx).map { chat ->
            chat.isMuted = PrefsManager.isContactMuted(ctx, chat.address)
            chat.isBlocked = PrefsManager.isContactBlocked(ctx, chat.address)
            chat
        }

        contactNameCache.clear()
        for (chat in allChats) {
            if (!contactNameCache.containsKey(chat.address)) {
                contactNameCache[chat.address] = SmsUtils.getContactDisplayName(ctx, chat.address)
            }
        }

        adapter = ChatListAdapter(allChats) { chat ->
            val intent = Intent(activity, ChatActivity::class.java).apply {
                putExtra("address", chat.address)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        filterChats(etSearch.text.toString())
    }

    private fun filterChats(query: String) {
        if (query.isBlank()) {
            adapter?.updateList(allChats)
            return
        }
        val lowerQuery = query.lowercase(Locale.ROOT)
        val filtered = allChats.filter { chat ->
            val name = contactNameCache[chat.address]?.lowercase(Locale.ROOT) ?: ""
            val lastMsg = chat.lastMessage.lowercase(Locale.ROOT)
            name.contains(lowerQuery) || lastMsg.contains(lowerQuery) || chat.address.contains(lowerQuery)
        }
        adapter?.updateList(filtered)
    }
}