package com.example.rmas_uross.data.repository

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.rmas_uross.data.model.ImageUpload
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class ImageUploadRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val uploadsCollection = db.collection("uploads")

    init {
        Log.d("ImageUploadRepository", "=== FIREBASE CONFIG ===")
        Log.d("ImageUploadRepository", "Storage Bucket: ${storage.app.options.storageBucket ?: "NOT SET"}")
        Log.d("ImageUploadRepository", "Project ID: ${storage.app.options.projectId}")
    }
    suspend fun uploadImage(bitmap: Bitmap, upload: ImageUpload): String {
        return try {
            val base64Image = bitmapToBase64(bitmap)
            Log.d("ImageUploadRepository", "Image converted to Base64: ${base64Image.length} chars")

            val uploadWithImage = upload.copy(
                imageBase64 = base64Image,
                imageUrl = "firestore_base64"
            )
            uploadsCollection.document(upload.id)
                .set(uploadWithImage)
                .await()

            Log.d("ImageUploadRepository", "Image saved to Firestore successfully")
            "firestore_success"

        } catch (e: Exception) {
            Log.e("ImageUploadRepository", "Error uploading to Firestore: ${e.message}", e)
            throw Exception("Greška pri čuvanju slike: ${e.message}")
        }
    }
    suspend fun getUserUploads(userId: String): List<ImageUpload> {
        return try {
            Log.d("ImageUploadRepository", "Getting user uploads for: $userId")
            val result = uploadsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(ImageUpload::class.java)
            Log.d("ImageUploadRepository", "Found ${result.size} uploads")
            result
        } catch (e: Exception) {
            Log.e("ImageUploadRepository", "Error getting user uploads: ${e.message}", e)
            emptyList()
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}