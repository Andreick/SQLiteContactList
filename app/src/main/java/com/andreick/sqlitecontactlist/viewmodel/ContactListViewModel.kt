package com.andreick.sqlitecontactlist.viewmodel

import androidx.lifecycle.ViewModel
import com.andreick.sqlitecontactlist.Contact
import com.andreick.sqlitecontactlist.repository.ContactListRepository

class ContactListViewModel(val repository: ContactListRepository? = null) : ViewModel() {

    fun searchContact(
        searchString: String,
        onSuccess: (List<Contact>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (searchString.isNotBlank()) {
            Thread {
                repository?.searchContacts(searchString, { searchedContacts ->
                    onSuccess(searchedContacts)
                }) { e -> onFailure(e) }
            }.start()
        }
    }
}