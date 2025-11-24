package com.example.rmas_uross.ui.pages.signup

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rmas_uross.navigation.Route
import com.example.rmas_uross.ui.auth.AuthViewModel
import com.example.rmas_uross.util.rememberImagePickerController
import java.io.ByteArrayOutputStream

@Composable
fun SignupScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imagePicker = rememberImagePickerController { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                var bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                bitmap = correctImageRotation(bitmap, context, uri)

                selectedImageBitmap = bitmap
            } catch (e: Exception) {
                Log.e("SignupScreen", "Error loading image", e)
                errorMessage = "Greška pri učitavanju slike"
            }
        }
    }
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Izaberite izvor slike") },
            text = { Text("Odakle želite da dodate profilnu sliku?") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            imagePicker.takePhoto()
                        }
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kamera")
                    }

                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            imagePicker.pickFromGallery()
                        }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Galerija")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImageSourceDialog = false }
                ) {
                    Text("Otkaži")
                }
            }
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
        Text(
            text = "Kreiraj Nalog",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageBitmap != null) {
                Image(
                    bitmap = selectedImageBitmap!!.asImageBitmap(),
                    contentDescription = "Profilna slika",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Dodaj sliku",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.BottomEnd)
                    .clickable { showImageSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Dodaj sliku",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Text(
            text = if (selectedImageBitmap != null) "Profilna slika" else "Dodaj profilnu sliku",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
            color = if (selectedImageBitmap != null) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Korisničko ime") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = username.isBlank() && errorMessage != null
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Ime i prezime") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = fullName.isBlank() && errorMessage != null
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = email.isBlank() && errorMessage != null
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Broj telefona") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = phoneNumber.isBlank() && errorMessage != null
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Šifra") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = password.length < 6 && errorMessage != null
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Potvrdi šifru") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = (password != confirmPassword) && errorMessage != null
            )
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    username.isBlank() -> {
                        errorMessage = "Unesite korisničko ime"
                        return@Button
                    }
                    fullName.isBlank() -> {
                        errorMessage = "Unesite ime i prezime"
                        return@Button
                    }
                    email.isBlank() -> {
                        errorMessage = "Unesite email"
                        return@Button
                    }
                    phoneNumber.isBlank() -> {
                        errorMessage = "Unesite broj telefona"
                        return@Button
                    }
                    password != confirmPassword -> {
                        errorMessage = "Šifre se ne poklapaju"
                        return@Button
                    }
                    password.length < 6 -> {
                        errorMessage = "Šifra mora imati najmanje 6 karaktera"
                        return@Button
                    }
                }

                isLoading = true
                errorMessage = null

                val imageBase64 = selectedImageBitmap?.let { bitmap ->
                    convertBitmapToBase64(bitmap)
                }

                authViewModel.signUp(
                    email = email,
                    password = password,
                    username = username,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    profileImageBase64 = imageBase64
                ) { success ->
                    isLoading = false
                    if (success) {
                        navController.navigate(Route.home) {
                            popUpTo(Route.login) { inclusive = true }
                        }
                    } else {
                        errorMessage = authViewModel.errorMessage.value ?: "Registracija nije uspela. Pokušajte ponovo."
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registracija...")
            } else {
                Text("Registruj se")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                navController.navigate(Route.login) {
                    popUpTo(Route.signup) { inclusive = true }
                }
            }
        ) {
            Text("Već imate nalog? Prijavite se")
        }
    }
}

private fun convertBitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    val imageBytes = outputStream.toByteArray()
    return android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT)
}

private fun correctImageRotation(bitmap: Bitmap, context: android.content.Context, uri: Uri): Bitmap {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            val exifInterface = android.media.ExifInterface(stream)
            val orientation = exifInterface.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = android.graphics.Matrix()
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            return android.graphics.Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            ).also { rotatedBitmap ->
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                }
            }
        }
        bitmap
    } catch (e: Exception) {
        Log.e("SignupScreen", "Error correcting image rotation", e)
        bitmap
    }
}