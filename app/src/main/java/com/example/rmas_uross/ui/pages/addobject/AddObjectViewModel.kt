package com.example.rmas_uross.ui.pages.addobject

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rmas_uross.data.model.AppObject
import com.example.rmas_uross.data.repository.ObjectRepository
import com.example.rmas_uross.data.repository.PointsRepository
import com.example.rmas_uross.util.PointsSystem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class SaveState {
    object Idle : SaveState()
    object Loading : SaveState()
    data class Success(val message: String, val pointsAwarded: Int = 0) : SaveState()
    data class Error(val message: String) : SaveState()
}

data class AddObjectUiState(
    val name: String = "",
    val description: String = "",
    val imageUri: Uri? = null,
    val type: String = "BENCH",
    val state: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val pointsAwarded: Int = 0
)

class AddObjectViewModel(
    private val repository: ObjectRepository,
    private val auth: FirebaseAuth,
    private val pointsRepository: PointsRepository = PointsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddObjectUiState())
    val uiState: StateFlow<AddObjectUiState> = _uiState.asStateFlow()
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()
    private val _pointsAwarded = MutableStateFlow(0)
    val pointsAwarded: StateFlow<Int> = _pointsAwarded.asStateFlow()

    fun updateLocation(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(latitude = latitude, longitude = longitude)
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun onDescriptionChange(newDescription: String) {
        _uiState.update { it.copy(description = newDescription) }
    }

    fun onTypeChange(newType: String) {
        _uiState.update { it.copy(type = newType) }
    }

    fun onStateChange(newState: String) {
        _uiState.update { it.copy(state = newState) }
    }

    fun onImageUriChange(newUri: Uri?) {
        _uiState.update { it.copy(imageUri = newUri) }
    }

    fun saveObject() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _saveState.value = SaveState.Error("Korisnik nije prijavljen.")
            return
        }

        val currentState = _uiState.value
        if (currentState.name.isBlank() ||
            currentState.latitude == null ||
            currentState.longitude == null
        ) {
            _saveState.value = SaveState.Error("Obavezna polja: Naziv i Lokacija")
            return
        }

        _saveState.value = SaveState.Loading

        viewModelScope.launch {
            try {
                val newObject = AppObject(
                    id = UUID.randomUUID().toString(),
                    name = currentState.name,
                    description = currentState.description,
                    type = currentState.type,
                    state = currentState.state,
                    latitude = currentState.latitude!!,
                    longitude = currentState.longitude!!,
                    userId = userId,
                    timestamp = System.currentTimeMillis(),
                    imageUrl = ""
                )

                val result = if (currentState.imageUri != null) {
                    repository.addObject(userId, newObject, currentState.imageUri)
                } else {
                    repository.addObject(newObject)
                }

                if (result.isFailure) {
                    _saveState.value =
                        SaveState.Error("Greška pri čuvanju: ${result.exceptionOrNull()?.message}")
                    return@launch
                }

                val pointsResult = pointsRepository.awardPoints(
                    userId = userId,
                    actionType = "ADD_OBJECT",
                    targetId = newObject.id
                )

                val points = pointsResult.getOrElse { PointsSystem.POINTS_ADD_OBJECT }

                _pointsAwarded.value = points

                _saveState.value = SaveState.Success(
                    message = "Objekat uspešno dodat! +$points poena",
                    pointsAwarded = points
                )

            } catch (e: Exception) {
                _saveState.value = SaveState.Error("Greška: ${e.message}")
            }
        }
    }
}