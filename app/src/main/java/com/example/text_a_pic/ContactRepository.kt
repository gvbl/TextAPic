package com.example.text_a_pic

import android.app.Application
import androidx.room.Room

class ContactRepository(application: Application) {

    private val contactDao: ContactDao

    init {
        val db = Room.databaseBuilder(
            application,
            AppDatabase::class.java, "text-a-pic-database"
        ).build()
        contactDao = db.contactDao()
    }

    fun selectAll() = contactDao.selectAll()

    fun insert(contact: Contact) = contactDao.insert(contact)

    fun delete(contact: Contact) = contactDao.delete(contact)
}