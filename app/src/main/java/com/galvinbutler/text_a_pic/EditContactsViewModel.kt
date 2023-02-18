package com.galvinbutler.text_a_pic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactRepository(application)

    val contacts = repository.selectAll()

    fun deleteContacts(contacts: List<Contact>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(contacts)
        }
    }
}