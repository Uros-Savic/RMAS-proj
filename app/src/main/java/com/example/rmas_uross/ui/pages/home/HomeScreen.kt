package com.example.rmas_uross.ui.pages.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rmas_uross.ui.auth.AuthViewModel
import com.example.rmas_uross.util.rememberImagePickerController
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseUser

@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel
) {
    val userData by authViewModel.userData.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val currentUpload by homeViewModel.currentUpload.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val uploadSuccess by homeViewModel.uploadSuccess.collectAsState()
    val errorMessage by homeViewModel.errorMessage.collectAsState()
    val userUploads by homeViewModel.userUploads.collectAsState()

    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userData, currentUser) {
        Log.d("HomeScreen", "UserData: $userData")
        Log.d("HomeScreen", "CurrentUser: $currentUser")
        Log.d("HomeScreen", "CurrentUser UID: ${currentUser?.uid}")

        userData?.let { user ->
            Log.d("HomeScreen", "Setting user info: ${user.uid}, ${user.fullName}")
            homeViewModel.setUserInfo(user)
        } ?: run {
            Log.d("HomeScreen", "UserData is null")
            currentUser?.let { firebaseUser ->
                val tempUser = com.example.rmas_uross.data.model.User(
                    uid = firebaseUser.uid,
                    username = firebaseUser.email?.substringBefore("@") ?: "user",
                    email = firebaseUser.email ?: "",
                    fullName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User"
                )
                Log.d("HomeScreen", "Using FirebaseUser: ${tempUser.uid}")
                homeViewModel.setUserInfo(tempUser)
            }
        }
    }

    val imagePicker = rememberImagePickerController { uri ->
        Log.d("HomeScreen", "Image picker callback called with URI: $uri")

        if (currentUser == null) {
            homeViewModel.setErrorMessage("Niste prijavljeni. Molimo prijavite se ponovo.")
            return@rememberImagePickerController
        }

        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedImageBitmap = bitmap
                inputStream?.close()
                showUploadDialog = true
                homeViewModel.setImageBitmap(bitmap)
                Log.d("HomeScreen", "Bitmap loaded and set in ViewModel")
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error loading image", e)
                homeViewModel.setErrorMessage("Greška pri učitavanju slike: ${e.message}")
            }
        } else {
            Log.d("HomeScreen", "URI is null")
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = {
                showImageSourceDialog = false
                Log.d("HomeScreen", "Image source dialog dismissed")
            },
            title = { Text("Dodaj fotografiju") },
            text = {
                Column {
                    Text("Odakle želite da dodate fotografiju?")
                    Spacer(modifier = Modifier.height(8.dp))
                    currentUser?.let { user ->
                        Text(
                            text = "Prijavljeni ste kao: ${user.email ?: user.uid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } ?: run {
                        Text(
                            text = "Niste prijavljeni!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            Log.d("HomeScreen", "Camera button clicked")
                            if (currentUser == null) {
                                homeViewModel.setErrorMessage("Morate biti prijavljeni da biste dodali fotografiju")
                                return@Button
                            }
                            showImageSourceDialog = false
                            imagePicker.takePhoto()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = currentUser != null
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Snimi fotografiju")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            Log.d("HomeScreen", "Gallery button clicked")
                            if (currentUser == null) {
                                homeViewModel.setErrorMessage("Morate biti prijavljeni da biste dodali fotografiju")
                                return@Button
                            }
                            showImageSourceDialog = false
                            imagePicker.pickFromGallery()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = currentUser != null
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Izaberi iz galerije")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        Log.d("HomeScreen", "Cancel button clicked")
                    }
                ) {
                    Text("Otkaži")
                }
            }
        )
    }

    if (showUploadDialog && selectedImageBitmap != null) {
        UploadDialog(
            upload = currentUpload,
            onTitleChange = { title ->
                homeViewModel.updateTitle(title)
                Log.d("HomeScreen", "Title updated: $title")
            },
            onDescriptionChange = { description ->
                homeViewModel.updateDescription(description)
                Log.d("HomeScreen", "Description updated: $description")
            },
            onLocationChange = { location ->
                homeViewModel.updateLocation(location)
                Log.d("HomeScreen", "Location updated: $location")
            },
            onUpload = {
                Log.d("HomeScreen", "Upload button clicked")
                if (currentUser == null) {
                    homeViewModel.setErrorMessage("Korisnik nije prijavljen. Molimo prijavite se ponovo.")
                    return@UploadDialog
                }

                selectedImageBitmap?.let { bitmap ->
                    coroutineScope.launch {
                        homeViewModel.uploadImage(bitmap) { success ->
                            Log.d("HomeScreen", "Upload completed: $success")
                            if (success) {
                                selectedImageBitmap = null
                                showUploadDialog = false
                            }
                        }
                    }
                }
            },
            onDismiss = {
                Log.d("HomeScreen", "Upload dialog dismissed")
                showUploadDialog = false
                selectedImageBitmap = null
                homeViewModel.resetUpload()
            },
            isLoading = isLoading,
            errorMessage = errorMessage,
            currentUser = currentUser
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {
        if (currentUser != null) {
            userData?.let { user ->
                Text(
                    text = "Dobrodošli,",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${user.fullName}!",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            Text(
                text = "Niste prijavljeni",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            ) {
                Text("Prijavite se")
            }
            return@Column
        }
    }
}

@Composable
fun UploadDialog(
    upload: com.example.rmas_uross.data.model.ImageUpload,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onUpload: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    currentUser: FirebaseUser?
) {
    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = { Text("Upload fotografije") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                currentUser?.let { user ->
                    Text(
                        text = "Korisnik: ${user.email ?: user.uid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                OutlinedTextField(
                    value = upload.title,
                    onValueChange = onTitleChange,
                    label = { Text("Naslov *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = upload.title.isBlank()
                )
                if (upload.title.isBlank()) {
                    Text(
                        text = "Naslov je obavezan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = upload.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Opis") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = upload.location,
                    onValueChange = onLocationChange,
                    label = { Text("Lokacija") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpload,
                enabled = !isLoading && upload.title.isNotBlank() && currentUser != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploaduje se...")
                } else {
                    Text("Uploaduj")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Otkaži")
            }
        }
    )
}

@Composable
fun UserUploadsList(
    uploads: List<com.example.rmas_uross.data.model.ImageUpload>,
    homeViewModel: HomeViewModel
) {
    if (uploads.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Vaše fotografije",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uploads) { upload ->
                        UploadItemCard(
                            upload = upload,
                            homeViewModel = homeViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UploadItemCard(
    upload: com.example.rmas_uross.data.model.ImageUpload,
    homeViewModel: HomeViewModel
) {
    var showImageDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showImageDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            if (upload.imageBase64.isNotBlank()) {
                val bitmap = remember(upload.imageBase64) {
                    homeViewModel.base64ToBitmap(upload.imageBase64)
                }

                bitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = upload.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Text(
                text = upload.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )

            if (upload.description.isNotBlank()) {
                Text(
                    text = upload.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (upload.location.isNotBlank()) {
                Text(
                    text = "📍 ${upload.location}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "📅 ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(upload.timestamp))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (showImageDialog && upload.imageBase64.isNotBlank()) {
        val bitmap = remember(upload.imageBase64) {
            homeViewModel.base64ToBitmap(upload.imageBase64)
        }

        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            title = { Text(upload.title) },
            text = {
                Column {
                    bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = upload.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (upload.description.isNotBlank()) {
                        Text(
                            text = upload.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (upload.location.isNotBlank()) {
                        Text(
                            text = "Lokacija: ${upload.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showImageDialog = false }
                ) {
                    Text("Zatvori")
                }
            }
        )
    }
}