package com.example.text_a_pic

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ContactDao {
    @Query("SELECT * FROM contact")
    fun selectAll(): LiveData<List<Contact>>

    @Upsert
    fun upsert(contact: Contact)

    @Delete
    fun delete(user: Contact)
}