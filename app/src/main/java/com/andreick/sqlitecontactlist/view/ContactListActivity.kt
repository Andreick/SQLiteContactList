package com.andreick.sqlitecontactlist.view

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.andreick.sqlitecontactlist.Contact
import com.andreick.sqlitecontactlist.ContactAdapter
import com.andreick.sqlitecontactlist.ContactListApplication
import com.andreick.sqlitecontactlist.R
import com.andreick.sqlitecontactlist.databinding.ActivityContactListBinding
import com.andreick.sqlitecontactlist.model.ContactRow
import com.andreick.sqlitecontactlist.database.DbHelper
import com.andreick.sqlitecontactlist.databinding.DialogSaveContactBinding
import com.andreick.sqlitecontactlist.repository.ContactListRepository
import com.andreick.sqlitecontactlist.viewmodel.ContactListViewModel

class ContactListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactListBinding
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactList: MutableList<Contact>
    private var viewModel: ContactListViewModel? = null
    private var dbHelper: DbHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (viewModel == null) viewModel =
            ContactListViewModel(ContactListRepository(DbHelper(this)))
        initDrawer()
        dbHelper = ContactListApplication.instance.dbHelper
        contactList = dbHelper?.fetchAllContacts()?.toMutableList() ?: mutableListOf()
        setupRecyclerView()
        setupListeners()
    }

    private fun initDrawer() {
        setSupportActionBar(binding.toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout, binding.toolbar,
            R.string.open_drawer, R.string.close_drawer
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupRecyclerView() {
        binding.rvContacts.layoutManager = LinearLayoutManager(this@ContactListActivity)
        setContactAdapter(contactList)
        /*binding.rvContacts.addItemDecoration(
            DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL)
        )*/
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
        viewModel?.let { viewModel ->
            val searchString = binding.etContactSearch.text.toString()
            binding.progressBar.visibility = View.VISIBLE
            viewModel.searchContact(searchString, { searchedContacts ->
                runOnUiThread {
                    setContactAdapter(searchedContacts)
                    binding.progressBar.visibility = View.GONE
                }
            }, { e ->
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Warning")
                        .setMessage(e.message)
                        .setPositiveButton("Ok") { alert, _ ->
                            alert.dismiss()
                        }
                        .show()
                }
            })
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
                this@ContactListActivity,
                "Name or phone cannot be blank",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    override fun onDestroy() {
        dbHelper?.close()
        super.onDestroy()
    }
}