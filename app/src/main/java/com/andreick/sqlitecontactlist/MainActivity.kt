package com.andreick.sqlitecontactlist

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.andreick.sqlitecontactlist.databinding.ActivityMainBinding
import com.andreick.sqlitecontactlist.databinding.DialogSaveContactBinding

class MainActivity : ToolbarBaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var contactAdapter: ContactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBackToolbar(binding.toolbar)
        generateContacts()
        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        with(binding) {
            rvContacts.layoutManager = LinearLayoutManager(this@MainActivity)
            contactAdapter = ContactAdapter(
                ContactSingleton.contactList,
                { id -> editContact(id) },
                { id -> onDeleteContact(id) }
            )
            rvContacts.adapter = contactAdapter
        }
    }

    private fun setupListeners() {
        with(binding) {
            fabAddContact.setOnClickListener { addContact() }
        }
    }

    private fun addContact() {
        val dialogBinding = DialogSaveContactBinding.inflate(layoutInflater)
        showSaveContactDialog("Add Contact", dialogBinding) {
            onSaveContact(dialogBinding) { name, phone ->
                val contact = Contact(ContactSingleton.contactList.last().id + 1, name, phone)
                ContactSingleton.contactList.add(contact)
            }
        }
    }

    private fun editContact(id: Int) {
        val pair = findContact(id)
        if (pair != null) {
            val (position, contact) = pair
            val dialogBinding = DialogSaveContactBinding.inflate(layoutInflater).apply {
                etContactName.setText(contact.name)
                etContactPhone.setText(contact.phone)
            }
            showSaveContactDialog("Edit Contact", dialogBinding) {
                onSaveContact(dialogBinding) { name, phone ->
                    contact.name = name
                    contact.phone = phone
                    contactAdapter.notifyItemChanged(position)
                }
            }
        } else {
            Log.e("Contact list", "Contact edit id not found")
        }
    }

    private fun onSaveContact(
        dialogBinding: DialogSaveContactBinding,
        saveContact: (name: String, phone: String) -> Unit
    ): Boolean {
        val name = dialogBinding.etContactName.text.toString()
        val phone = dialogBinding.etContactPhone.text.toString()
        return if (name.isNotBlank() && phone.isNotBlank()) {
            saveContact(name, phone)
            true
        } else {
            Toast.makeText(
                this@MainActivity,
                "Name or phone cannot be blank",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    private fun findContact(id: Int): Pair<Int, Contact>? {
        var pair: Pair<Int, Contact>? = null
        for ((position, contact) in ContactSingleton.contactList.withIndex()) {
            if (contact.id == id) {
                pair = Pair(position, contact)
                break
            }
        }
        return pair
    }

    private fun onDeleteContact(id: Int) {
        val position = ContactSingleton.contactList.indexOfFirst { it.id == id }
        if (position >= 0) {
            ContactSingleton.contactList.removeAt(position)
            contactAdapter.notifyItemChanged(position)
        } else {
            Log.e("Contact list", "Contact delete id not found")
        }
    }

    private fun generateContacts() {
        with(ContactSingleton.contactList) {
            add(Contact(1, "Fulano", "(99) 91234-5678"))
            add(Contact(2, "Beltrano", "(99) 923456789"))
            add(Contact(3, "Sicrano", "(99) 934567890"))
        }
    }

    private fun showSaveContactDialog(
        title: String,
        dialogBinding: DialogSaveContactBinding,
        onSaveContactListener: () -> Boolean
    ) {
        val contactDialog = Dialog(this, R.style.Theme_CustomDialog)
        with(dialogBinding) {
            tvContactTitle.text = title
            tvSaveContact.setOnClickListener {
                if (onSaveContactListener()) contactDialog.dismiss()
            }
            tvCancelContact.setOnClickListener { contactDialog.dismiss() }
        }
        with(contactDialog) {
            setContentView(dialogBinding.root)
            setCancelable(false)
            show()
        }
    }
}