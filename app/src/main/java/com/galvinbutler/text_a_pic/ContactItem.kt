package com.galvinbutler.text_a_pic

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ContactItem(modifier: Modifier = Modifier, contact: Contact) {
    Column(modifier = modifier) {
        Text(text = contact.name, fontWeight = FontWeight.Bold)
        Text(text = contact.phoneNumber)
    }
}

@Preview(showBackground = true)
@Composable
fun ContactItemPreview() {
    ContactItem(contact = Contact(0, "Alice", "1 (555) 555-5555"))
}