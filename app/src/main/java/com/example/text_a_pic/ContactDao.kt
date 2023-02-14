package com.example.text_a_pic

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactDao {
    @Query("SELECT * FROM contact")
    fun selectAll(): LiveData<List<Contact>>

    @Insert
    fun insert(contact: Contact)

    @Delete
    fun delete(user: Contact)
}