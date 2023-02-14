package com.example.text_a_pic

import android.Manifest.permission.READ_CONTACTS
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
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

    private val contactPickerLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) {
        val viewModel: MainViewModel by viewModels()
        viewModel.resolveContact(it!!)
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
        TextAPicTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                Column {
                    Box {
                        Button(onClick = {
                            when(ContextCompat.checkSelfPermission(this@MainActivity, READ_CONTACTS)) {
                                PERMISSION_GRANTED -> {
                                    contactPickerLauncher.launch(null)
                                }
                                else -> readContactsPermissionLauncher.launch(READ_CONTACTS)
                            }
                        }) {
                            Text(text = "Select a contact")
                        }
                    }
                    ContactsList(viewModel)
                }
            }
        }
    }

    @Composable
    fun ContactsList(viewModel: MainViewModel) {
        val contacts = viewModel.contacts.observeAsState(emptyList())
        Column {
            for (contact in contacts.value) {
                Column {
                    Text(text = contact.name)
                    Text(text = contact.phoneNumber)
                    Button(onClick = { viewModel.deleteContact(contact) }) {
                        Text(text = "-")
                    }
                }
            }
        }
    }
}