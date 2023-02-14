package com.example.text_a_pic

import android.app.Application
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContentResolverCompat
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val contentResolver = application.contentResolver

    private val repository = ContactRepository(application)

    val contacts = repository.selectAll()

    fun deleteContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(contact)
        }
    }

    fun resolveContact(uri: Uri) {
        ContentResolverCompat.query(
            contentResolver,
            uri,
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor != null && cursor.count > 0 && cursor.moveToFirst()) {
                val name = cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY))!!
                if (cursor.getIntOrNull(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))!! == 1) {
                    val phoneNumber = resolvePhoneNumber(cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY))!!)
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.insert(Contact(name = name, phoneNumber = phoneNumber))
                    }
                }
            }
        }
    }

    private fun resolvePhoneNumber(lookupKey: String): String {
        val selection = "${ContactsContract.Data.LOOKUP_KEY} = ? AND ${ContactsContract.Data.MIMETYPE} = '${ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE}'"
        val selectionArgs: Array<String> = arrayOf(lookupKey)
        val sortOrder = ContactsContract.Data.MIMETYPE
        ContentResolverCompat.query(
            contentResolver,
            ContactsContract.Data.CONTENT_URI,
            null,
            selection,
            selectionArgs,
            sortOrder,
            null,
        ).use { cursor ->
            if (cursor != null && cursor.count > 0 && cursor.moveToFirst()) {
                return cursor.getStringOrNull(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )!!
            }
        }
        throw Exception("should not get here")
    }
}