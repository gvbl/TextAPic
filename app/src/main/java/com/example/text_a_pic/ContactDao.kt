package com.example.text_a_pic

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ContactDao {
    @Query("SELECT * FROM contact")
    fun selectAll(): LiveData<List<Contact>>

    @Query("SELECT * FROM contact WHERE id = :id")
    fun findById(id: Int): Contact

    @Upsert
    fun upsert(contact: Contact)

    @Delete
    fun delete(user: Contact)
}