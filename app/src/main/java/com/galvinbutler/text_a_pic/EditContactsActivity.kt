@file:OptIn(ExperimentalMaterialApi::class)

package com.galvinbutler.text_a_pic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.galvinbutler.text_a_pic.ui.theme.TextAPicTheme

class EditContactsActivity : ComponentActivity() {

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, EditContactsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: EditContactsViewModel by viewModels()
        setContent {
            EditContacts(viewModel)
        }
    }

    @Composable
    fun EditContacts(viewModel: EditContactsViewModel) {
        val contacts by viewModel.contacts.observeAsState(emptyList())
        val checked = remember { mutableStateListOf<Int>() }
        TextAPicTheme {
            Surface(
                color = MaterialTheme.colors.background
            ) {
                Column {
                    EditAppBar()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        contacts.forEach { contact ->
                            Surface(modifier = Modifier.fillMaxWidth(), onClick = {
                                if (checked.contains(contact.id)) {
                                    checked.remove(contact.id)
                                } else {
                                    checked.add(contact.id)
                                }
                            }) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    ), verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked.contains(contact.id),
                                        onCheckedChange = null
                                    )
                                    ContactItem(
                                        modifier = Modifier.padding(start = 16.dp),
                                        contact = contact
                                    )
                                }
                            }
                        }
                    }
                }
                if (checked.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            onClick = {
                                viewModel.deleteContacts(contacts.filter { checked.contains(it.id) })
                            },
                            text = { Text(text = getString(R.string.delete)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun EditAppBar() {
        TopAppBar(
            title = { Text(text = getString(R.string.edit_contacts)) },
            navigationIcon = {
                IconButton(onClick = {
                    finish()
                }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                }
            }
        )
    }
}