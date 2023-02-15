@file:OptIn(ExperimentalMaterialApi::class)

package com.example.text_a_pic

import android.Manifest.permission.READ_CONTACTS
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.example.text_a_pic.ui.theme.TextAPicTheme

class MainActivity : ComponentActivity() {

    private val readContactsPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                contactPickerLauncher.launch(null)
            } else {
                // TODO: show snackbar telling user this permission is required
            }
        }

    private val contactPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickContact()) {
            val viewModel: MainViewModel by viewModels()
            it?.run {
                viewModel.resolveContact(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: MainViewModel by viewModels()

        setContent {
            MainApp(viewModel)
        }
    }

    @Composable
    fun MainApp(viewModel: MainViewModel) {
        val selected by viewModel.selectedContact.observeAsState(null)
        TextAPicTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                selected?.let {
                    Column() {
                        MainAppBar()
                        ContactsDropdown(viewModel, it)
                    }
                } ?: AddContact()
            }
        }
    }

    @Composable
    fun MainAppBar() {
        var showMenu by remember { mutableStateOf(false) }
        TopAppBar(
            title = { Text(text = getString(R.string.app_name)) },
            actions = {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(onClick = {
                        startActivity(EditContactsActivity.newIntent(this@MainActivity))
                        showMenu = false
                    }) {
                        Text(text = getString(R.string.edit_contacts))
                    }
                }
            }
        )
    }

    @Composable
    fun AddContact() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    when (ContextCompat.checkSelfPermission(this@MainActivity, READ_CONTACTS)) {
                        PERMISSION_GRANTED -> {
                            contactPickerLauncher.launch(null)
                        }
                        else -> readContactsPermissionLauncher.launch(READ_CONTACTS)
                    }
                },
                text = { Text(text = getString(R.string.add_contact)) },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) }
            )
        }
    }

    @Composable
    fun ContactsDropdown(viewModel: MainViewModel, selected: Contact) {
        val contacts by viewModel.contacts.observeAsState(emptyList())
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            Column(modifier = Modifier.width(200.dp)) {
                Text(text = selected.name)
                Text(text = selected.phoneNumber)
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                contacts.forEach { contact ->
                    DropdownMenuItem(onClick = {
                        viewModel.selectContact(contact)
                        expanded = false
                    }) {
                        Column() {
                            Text(text = contact.name)
                            Text(text = contact.phoneNumber)
                        }
                    }
                }
                DropdownMenuItem(onClick = {
                    contactPickerLauncher.launch(null)
                    expanded = false
                }) {
                    Row {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Text(text = getString(R.string.add_new_contact))
                    }
                }
            }
        }
    }
}