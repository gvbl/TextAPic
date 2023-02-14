package com.example.text_a_pic

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Contact(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phoneNumber") val phoneNumber: String
) {
    override fun toString(): String {
        return "Contact(id=$id, name=${name}, phoneNumber=$phoneNumber)"
    }
}