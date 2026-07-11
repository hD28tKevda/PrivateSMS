package com.example.smscrypto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ContactsListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnClear: ImageButton
    private var allContacts: List<ContactInfo> = emptyList()
    private var filteredContacts: List<ContactInfo> = emptyList()
    private var adapter: ContactAdapter? = null

    data class ContactInfo(val name: String, val phone: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts_list)

        recyclerView = findViewById(R.id.recycler_contacts)
        etSearch = findViewById(R.id.et_search)
        btnClear = findViewById(R.id.btn_clear_search)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Кнопка назад
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Поиск
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                btnClear.visibility = if (query.isBlank()) View.GONE else View.VISIBLE
                filterContacts(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClear.setOnClickListener { etSearch.text.clear() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 400)
        } else {
            loadContacts()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 400 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadContacts()
        }
    }

    private fun loadContacts() {
        val contactList = mutableListOf<ContactInfo>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0) ?: "Unknown"
                val phone = it.getString(1) ?: continue
                contactList.add(ContactInfo(name, phone))
            }
        }

        // Убираем дубликаты по номеру, сортируем по имени
        allContacts = contactList.distinctBy { it.phone }
            .sortedBy { it.name.lowercase(Locale.ROOT) }

        filteredContacts = allContacts
        adapter = ContactAdapter(filteredContacts) { contact ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("address", contact.phone)
            }
            startActivity(intent)
            finish()
        }
        recyclerView.adapter = adapter
    }

    private fun filterContacts(query: String) {
        if (query.isBlank()) {
            filteredContacts = allContacts
        } else {
            val lowerQuery = query.lowercase(Locale.ROOT)
            filteredContacts = allContacts.filter {
                it.name.lowercase(Locale.ROOT).contains(lowerQuery) ||
                        it.phone.contains(lowerQuery)
            }
        }
        adapter?.updateList(filteredContacts)
    }
}

// Адаптер теперь в отдельном классе или можно оставить внутри, но я вынесу ниже
class ContactAdapter(
    private var contacts: List<ContactsListActivity.ContactInfo>,
    private val onItemClick: (ContactsListActivity.ContactInfo) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.iv_avatar)
        val name: TextView = view.findViewById(R.id.tv_name)
        val phone: TextView = view.findViewById(R.id.tv_phone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        holder.phone.text = contact.phone
        holder.itemView.setOnClickListener { onItemClick(contact) }
    }

    override fun getItemCount() = contacts.size

    fun updateList(newList: List<ContactsListActivity.ContactInfo>) {
        contacts = newList
        notifyDataSetChanged()
    }
}