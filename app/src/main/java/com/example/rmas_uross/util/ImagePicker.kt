package com.example.rmas_uross.util

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun rememberImagePickerController(
    onImageSelected: (Uri?) -> Unit
): ImagePickerController {
    val context = LocalContext.current
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraImageUri?.let { uri ->
                onImageSelected(uri)
            }
        } else {
            onImageSelected(null)
        }
    }

    return remember {
        ImagePickerController(
            context = context,
            galleryLauncher = galleryLauncher,
            cameraLauncher = cameraLauncher,
            onUriCreated = { uri -> cameraImageUri = uri }
        )
    }
}

class ImagePickerController(
    private val context: Context,
    private val galleryLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    private val cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    private val onUriCreated: (Uri) -> Unit
) {
    fun pickFromGallery() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
        }
    }

    fun takePhoto() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"

            val storageDir = context.cacheDir
            val imageFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )

            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            onUriCreated(imageUri)

            cameraLauncher.launch(imageUri)
        } catch (e: Exception) {
        }
    }
}