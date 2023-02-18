package com.galvinbutler.text_a_pic

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.galvinbutler.text_a_pic.Contact
import com.galvinbutler.text_a_pic.ContactRepository
import com.galvinbutler.text_a_pic.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactRepository(application)

    val contacts = repository.selectAll()
    val selectedContact = repository.recipientId.map { id ->
        id?.let { Resource.success(repository.findById(it)) } ?: Resource.success(null)
    }.flowOn(Dispatchers.IO).asLiveData()

    fun resolveContact(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val contact = repository.resolveContact(uri)
            repository.upsert(contact)
            repository.setRecipientId(contact.id)
        }
    }

    fun selectContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setRecipientId(contact.id)
        }
    }
}