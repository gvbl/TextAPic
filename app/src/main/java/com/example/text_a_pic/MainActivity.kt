@file:OptIn(ExperimentalMaterialApi::class)

package com.example.text_a_pic

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_CONTACTS
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.example.text_a_pic.ui.theme.TextAPicTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

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

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // TODO: ?
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

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
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
                        MainAppBar(viewModel, it)
                        Camera()

                    }
                } ?: AddContact()
            }
        }
    }

    @Composable
    fun MainAppBar(viewModel: MainViewModel, selected: Contact) {
        var showMenu by remember { mutableStateOf(false) }
        TopAppBar(
            title = {
                ContactsDropdown(viewModel, selected)
            },
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactItem(contact = selected)
                Icon(
                    modifier = Modifier.padding(start = 8.dp),
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }
            ExposedDropdownMenu(
                modifier = Modifier.width(200.dp),
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                contacts.forEach { contact ->
                    DropdownMenuItem(onClick = {
                        viewModel.selectContact(contact)
                        expanded = false
                    }) {
                        ContactItem(contact = contact)
                    }
                }
                DropdownMenuItem(onClick = {
                    contactPickerLauncher.launch(null)
                    expanded = false
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Text(modifier = Modifier.padding(start = 8.dp), text = getString(R.string.add_new_contact), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    @Composable
    fun Camera() {
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(this) }

        when (ContextCompat.checkSelfPermission(this@MainActivity, CAMERA)) {
            PERMISSION_GRANTED -> {
                AndroidView(factory = { context ->
                    val previewView = PreviewView(context)
                    val executor = ContextCompat.getMainExecutor(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                    }, executor)
                    previewView
                }, modifier = Modifier.fillMaxSize())
            }
            else -> Button(onClick = {
                cameraPermissionLauncher.launch(CAMERA)
            }) {
                Text(text = getString(R.string.enable_camera))
            }
        }
    }
}