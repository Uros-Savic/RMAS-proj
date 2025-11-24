package com.example.rmas_uross.ui.pages.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rmas_uross.data.model.ImageUpload
import com.example.rmas_uross.data.model.User
import com.example.rmas_uross.data.repository.ImageUploadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val uploadRepository: ImageUploadRepository
) : ViewModel() {

    private val _currentUpload = MutableStateFlow(ImageUpload.createEmpty())
    val currentUpload: StateFlow<ImageUpload> = _currentUpload.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _userUploads = MutableStateFlow<List<ImageUpload>>(emptyList())
    val userUploads: StateFlow<List<ImageUpload>> = _userUploads.asStateFlow()
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    fun updateTitle(title: String) {
        _currentUpload.value = _currentUpload.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _currentUpload.value = _currentUpload.value.copy(description = description)
    }

    fun updateLocation(location: String) {
        _currentUpload.value = _currentUpload.value.copy(location = location)
    }

    fun setUserInfo(user: User) {
        _currentUserId.value = user.uid
        _currentUpload.value = _currentUpload.value.copy(
            userId = user.uid,
            userName = user.fullName
        )
        fetchUserUploads(user.uid)
    }

    fun setImageBitmap(bitmap: Bitmap) {
        val base64 = uploadRepository.bitmapToBase64(bitmap)
        _currentUpload.value = _currentUpload.value.copy(imageBase64 = base64)
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    fun fetchUserUploads(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val uploads = uploadRepository.getUserUploads(userId)
                _userUploads.value = uploads
            } catch (e: Exception) {
                _errorMessage.value = "Greška pri učitavanju upload-a: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadImage(bitmap: Bitmap, onComplete: (Boolean) -> Unit) {
        val userId = _currentUserId.value
        if (userId.isNullOrBlank()) {
            _errorMessage.value = "Korisnik nije prijavljen. Molimo prijavite se ponovo."
            onComplete(false)
            return
        }

        if (_currentUpload.value.title.isBlank()) {
            _errorMessage.value = "Unesite naslov"
            onComplete(false)
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _uploadSuccess.value = false

        viewModelScope.launch {
            try {
                val uploadWithUser = _currentUpload.value.copy(
                    userId = userId,
                    userName = _currentUpload.value.userName.ifBlank { "Unknown User" }
                )

                val imageUrl = uploadRepository.uploadImage(bitmap, uploadWithUser)
                _uploadSuccess.value = true
                fetchUserUploads(userId)
                _currentUpload.value = ImageUpload.createEmpty()
                onComplete(true)
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("object does not exist") == true ->
                        "Greška: Firebase Storage nije pravilno konfigurisan"
                    e.message?.contains("permission") == true ->
                        "Greška: Nemate dozvolu za upload"
                    e.message?.contains("network") == true ->
                        "Greška: Problemi sa mrežom"
                    e.message?.contains("not signed in") == true ->
                        "Greška: Korisnik nije prijavljen"
                    else -> "Greška pri uploadu: ${e.message ?: "Nepoznata greška"}"
                }
                _errorMessage.value = errorMsg
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetUpload() {
        _currentUpload.value = ImageUpload.createEmpty()
        _uploadSuccess.value = false
        _errorMessage.value = null
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}