package com.example.text_a_pic

import android.app.Application
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContentResolverCompat
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactRepository(application)

    val contacts = repository.selectAll()
    val selectedContact = repository.recipientId.distinctUntilChanged().map { id ->
        contacts.value?.firstOrNull { it.id == id }
    }

    fun resolveContact(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val contact = repository.resolveContact(uri)
            repository.setRecipientId(contact.id)
            repository.insert(contact)
        }
    }

    fun selectContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setRecipientId(contact.id)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(contact)
        }
    }
}