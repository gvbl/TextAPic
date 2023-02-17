@file:OptIn(ExperimentalMaterialApi::class)

package com.example.text_a_pic

import android.Manifest.permission.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.example.text_a_pic.ui.theme.TextAPicTheme
import com.klinker.android.send_message.Transaction
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Todo: Account for "allow once" permissions, kicking back to permissions screen if any not granted
class MainActivity : ComponentActivity() {

    companion object {
        private val permissions = mutableListOf(
            READ_CONTACTS,
            CAMERA,
            READ_SMS,
            SEND_SMS,
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private val allPermissionsGrantedLiveData = MutableLiveData(false)

    private val permissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            allPermissionsGrantedLiveData.value = permissions.all { it.value }
        }

    private val contactPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickContact()) {
            val viewModel: MainViewModel by viewModels()
            it?.run {
                viewModel.resolveContact(it)
            }
        }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.v("Send MMS result code: $resultCode")
        }
    }

    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        allPermissionsGrantedLiveData.value = permissions.all {
            ContextCompat.checkSelfPermission(
                baseContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

        setContent {
            val viewModel: MainViewModel by viewModels()
            App(viewModel)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Transaction.MMS_SENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        unregisterReceiver(receiver)
    }

    @Composable
    fun App(viewModel: MainViewModel) {
        val allPermissionsGranted by allPermissionsGrantedLiveData.observeAsState(false)
        TextAPicTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                if (allPermissionsGranted) {
                    Main(viewModel)
                } else {
                    PermissionsIntro()
                }
            }
        }
    }

    @Composable
    fun PermissionsIntro() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = getString(R.string.app_name), style = MaterialTheme.typography.h4)
            Text(text = getString(R.string.permissions_header_1))
            Text(text = getString(R.string.permissions_header_2))
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = getString(R.string.take_photos)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = getString(R.string.read_contacts)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Folder, contentDescription = null)
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = getString(R.string.write_files)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Sms, contentDescription = null)
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = getString(R.string.send_and_view_sms)
                    )
                }
            }
            Button(onClick = {
                permissionsLauncher.launch(permissions)
            }) {
                Text(text = getString(R.string.grant_permissions))
            }
        }
    }

    @Composable
    fun Main(viewModel: MainViewModel) {
        val contactResource by viewModel.selectedContact.observeAsState(Resource.loading())
        if (contactResource.status == Status.SUCCESS) {
            contactResource.data?.let {
                Column {
                    MainAppBar(viewModel, it)
                    Camera()
                }
                Capture(viewModel, it)
            } ?: AddContact()
        }
    }

    @Composable
    fun MainAppBar(viewModel: MainViewModel, contact: Contact) {
        var showMenu by remember { mutableStateOf(false) }
        TopAppBar(
            modifier = Modifier.height(72.dp),
            title = {
                ContactsDropdown(viewModel = viewModel, selected = contact)
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
                    contactPickerLauncher.launch(null)
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
                modifier = Modifier.width(240.dp),
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                contacts.forEach { contact ->
                    DropdownMenuItem(
                        modifier = Modifier.padding(vertical = 8.dp),
                        onClick = {
                            viewModel.selectContact(contact)
                            expanded = false
                        }
                    ) {
                        ContactItem(modifier = Modifier.padding(vertical = 8.dp), contact = contact)
                    }
                }
                DropdownMenuItem(modifier = Modifier.padding(vertical = 16.dp), onClick = {
                    contactPickerLauncher.launch(null)
                    expanded = false
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = getString(R.string.add_new_contact),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun Camera() {
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(this) }

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
                    preview,
                    imageCapture
                )
            }, executor)
            previewView
        }, modifier = Modifier.fillMaxSize())
        imageCapture = ImageCapture.Builder().build()
    }

    @Composable
    fun Capture(viewModel: MainViewModel, contact: Contact) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            FloatingActionButton(
                modifier = Modifier.align(Alignment.BottomCenter),
                onClick = {
                    takePhoto(onSuccess = {
                        viewModel.sendPhoto(this@MainActivity, contact, it)
                    }, onError = {
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
                },
            ) {
                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
            }
        }
    }

    // https://developer.android.com/codelabs/camerax-getting-started#4
    private fun takePhoto(
        onSuccess: ((File) -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture!!

        // Create time stamped name
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val file = File(externalMediaDirs.first(), name)
        Timber.v("Capturing photo to file: ${file.absolutePath}")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(file)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    onError?.invoke(exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSuccess?.invoke(file)
                }
            }
        )
    }
}