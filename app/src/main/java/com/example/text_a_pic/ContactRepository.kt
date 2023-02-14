package com.example.text_a_pic

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContentResolverCompat
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.asLiveData
import androidx.room.Room
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ContactRepository(application: Application) {

    private val contentResolver = application.contentResolver
    private val contactDao: ContactDao
    private val context = application.applicationContext
    private val Context.dataStore by preferencesDataStore("text-a-pic-preferences")
    val RECIPIENT_ID = intPreferencesKey("recipient-id")

    init {
        val db = Room.databaseBuilder(
            application,
            AppDatabase::class.java, "text-a-pic-database"
        ).build()
        contactDao = db.contactDao()
    }

    fun resolveContact(uri: Uri): Contact {
        var id: Int? = null
        var name: String? = null
        var phoneNumber: String? = null
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
                name =
                    cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY))!!
                if (cursor.getIntOrNull(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))!! == 1) {
                    val selection =
                        "${ContactsContract.Data.LOOKUP_KEY} = ? AND ${ContactsContract.Data.MIMETYPE} = '${ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE}'"
                    val selectionArgs: Array<String> =
                        arrayOf(cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY))!!)
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
                            id =
                                cursor.getIntOrNull(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID))!!
                            phoneNumber =
                                cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))!!
                        }
                    }
                }
            }
        }
        return Contact(id = id!!, name = name!!, phoneNumber = phoneNumber!!)
    }

    val recipientId = context.dataStore.data.map { preferences -> preferences[RECIPIENT_ID] }
        .distinctUntilChanged().asLiveData()

    suspend fun setRecipientId(id: Int) {
        context.dataStore.edit { preferences ->
            preferences[RECIPIENT_ID] = id
        }
    }

    fun selectAll() = contactDao.selectAll()

    fun upsert(contact: Contact) = contactDao.upsert(contact)

    fun delete(contact: Contact) = contactDao.delete(contact)
}