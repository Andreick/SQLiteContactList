package com.andreick.sqlitecontactlist

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.andreick.sqlitecontactlist.data.ContactRow
import com.andreick.sqlitecontactlist.database.DbHelper
import com.andreick.sqlitecontactlist.databinding.ActivityMainBinding
import com.andreick.sqlitecontactlist.databinding.DialogSaveContactBinding

class MainActivity : ToolbarBaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactList: MutableList<Contact>
    private var dbHelper: DbHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBackToolbar(binding.toolbar)
        dbHelper = ContactListApplication.instance.dbHelper
        contactList = dbHelper?.fetchAllContacts()?.toMutableList() ?: mutableListOf()
        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        binding.rvContacts.layoutManager = LinearLayoutManager(this@MainActivity)
        setContactAdapter(contactList)
        binding.rvContacts.addItemDecoration(
            DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL)
        )
    }

    private fun setNoRecordsMessageVisibility(contactList: List<Contact>) {
        binding.tvNoRecords.visibility = if (contactList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setContactAdapter(contactList: List<Contact>) {
        contactAdapter = ContactAdapter(
            contactList,
            { id -> editContact(id) },
            { id -> deleteContact(id) }
        )
        binding.rvContacts.adapter = contactAdapter
        setNoRecordsMessageVisibility(contactList)
    }

    private fun setupListeners() {
        binding.fabAddContact.setOnClickListener { addContact() }
        binding.ivContactSearch.setOnClickListener { searchContact() }
    }

    private fun addContact() {
        val dialogBinding = DialogSaveContactBinding.inflate(layoutInflater)
        showSaveContactDialog("Add Contact", dialogBinding) {
            onSaveContact(dialogBinding) { (_, name, phone) ->
                dbHelper?.let { helper ->
                    runOnBackgroundWithProgressBar({
                        helper.insertContact(name, phone).toInt()
                    }, { rowId ->
                        Log.d("Inserted Contact", "$rowId")
                        if (rowId > -1) {
                            val insertionPosition = contactList.size
                            contactList.add(Contact(rowId, name, phone))
                            contactAdapter.notifyItemInserted(insertionPosition)
                            binding.tvNoRecords.visibility = View.GONE
                        }
                    })
                }
            }
        }
    }

    private fun editContact(id: Int) {
        val contactRow = getContactRowOrNull(id)
        if (contactRow != null) {
            val dialogBinding = DialogSaveContactBinding.inflate(layoutInflater).apply {
                etContactName.setText(contactRow.contact.name)
                etContactPhone.setText(contactRow.contact.phone)
            }
            showSaveContactDialog("Edit Contact", dialogBinding) {
                onSaveContact(dialogBinding, id) { updatedContact ->
                    dbHelper?.let { helper ->
                        runOnBackgroundWithProgressBar({
                            helper.updateContact(updatedContact)
                        }, { numUpdatedRows ->
                            if (numUpdatedRows == 1) {
                                contactRow.contact.apply {
                                    name = updatedContact.name
                                    phone = updatedContact.phone
                                }
                                contactAdapter.notifyItemChanged(contactRow.position)
                            }
                        })
                    }
                }
            }
        } else {
            Log.e("Contact list", "Contact edit id not found")
        }
    }

    private fun deleteContact(id: Int) {
        val contactRow = getContactRowOrNull(id)
        if (contactRow != null) {
            dbHelper?.let { helper ->
                runOnBackgroundWithProgressBar({
                    helper.deleteContactById(contactRow.contact.id)
                }, { numDeletedRows ->
                    if (numDeletedRows == 1) {
                        contactList.removeAt(contactRow.position)
                        contactAdapter.notifyItemRemoved(contactRow.position)
                        setNoRecordsMessageVisibility(contactList)
                    }
                })
            }
        } else {
            Log.e("Contact list", "Contact delete id not found")
        }
    }

    private fun searchContact() {
        val searchString = binding.etContactSearch.text.toString()
        if (searchString.isNotBlank()) {
            dbHelper?.let { helper ->
                runOnBackgroundWithProgressBar({
                    binding.tvNoRecords.visibility = View.GONE
                    helper.searchContacts(searchString)
                }, { searchedContacts ->
                    setContactAdapter(searchedContacts)
                })
            }
        } else {
            setContactAdapter(contactList)
        }
    }

    private fun <T> runOnBackgroundWithProgressBar(backgroundBlock: () -> T, uiBlock: (T) -> Unit) {
        binding.progressBar.visibility = View.VISIBLE
        Thread {
            val result = backgroundBlock()
            runOnUiThread {
                uiBlock(result)
                binding.progressBar.visibility = View.GONE
            }
        }.start()
    }

    private fun getContactRowOrNull(id: Int): ContactRow? {
        var contactRow: ContactRow? = null
        for ((position, contact) in contactList.withIndex()) {
            if (contact.id == id) {
                contactRow = ContactRow(position, contact)
                break
            }
        }
        return contactRow
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

    private fun onSaveContact(
        dialogBinding: DialogSaveContactBinding,
        id: Int = -1,
        saveContact: (contact: Contact) -> Unit
    ): Boolean {
        val name = dialogBinding.etContactName.text.toString()
        val phone = dialogBinding.etContactPhone.text.toString()
        return if (name.isNotBlank() && phone.isNotBlank()) {
            val contact = Contact(id, name, phone)
            saveContact(contact)
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

    private fun generateContacts() {
        with(ContactSingleton.contactList) {
            add(Contact(1, "Fulano", "(99) 91234-5678"))
            add(Contact(2, "Beltrano", "(99) 923456789"))
            add(Contact(3, "Sicrano", "(99) 934567890"))
        }
    }

    override fun onDestroy() {
        dbHelper?.close()
        super.onDestroy()
    }
}