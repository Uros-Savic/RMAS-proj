package com.example.rmas_uross.ui.auth

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rmas_uross.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class AuthViewModel : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val _currentUser = MutableStateFlow(firebaseAuth.currentUser)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()
    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _isUserLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
            _isUserLoggedIn.value = auth.currentUser != null

            auth.currentUser?.let { user ->
                viewModelScope.launch {
                    fetchUserData(user.uid)
                }
            } ?: run {
                _userData.value = null
            }
        }
        firebaseAuth.currentUser?.let { user ->
            viewModelScope.launch {
                fetchUserData(user.uid)
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        username: String,
        fullName: String,
        phoneNumber: String,
        profileImageBase64: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                Log.d("AuthViewModel", "Starting sign up process...")

                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user == null) {
                    _errorMessage.value = "Greška pri kreiranju naloga"
                    onComplete(false)
                    return@launch
                }

                Log.d("AuthViewModel", "Firebase user created: ${user.uid}")

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()

                user.updateProfile(profileUpdates).await()
                Log.d("AuthViewModel", "Firebase profile updated")

                val newUser = User(
                    uid = user.uid,
                    username = username,
                    email = email,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    profileImageUrl = "", // za brisanje kad me ne mrzi
                    profileImageBase64 = profileImageBase64 ?: "",
                    points = 0,
                    level = 1,
                    createdAt = System.currentTimeMillis(),
                    lastLogin = System.currentTimeMillis()
                )

                usersCollection.document(user.uid).set(newUser).await()
                Log.d("AuthViewModel", "User data saved to Firestore")

                _currentUser.value = user
                _userData.value = newUser
                _isLoading.value = false

                Log.d("AuthViewModel", "Sign up completed successfully")
                onComplete(true)

            } catch (e: Exception) {
                _isLoading.value = false
                Log.e("AuthViewModel", "Sign up error: ${e.message}", e)

                _errorMessage.value = when {
                    e.message?.contains("email address is already in use") == true ->
                        "Email adresa je već u upotrebi"
                    e.message?.contains("password is weak") == true ->
                        "Lozinka je previše slaba"
                    e.message?.contains("invalid email") == true ->
                        "Nevalidna email adresa"
                    e.message?.contains("network error") == true ->
                        "Problem sa mrežom. Proverite internet konekciju"
                    else -> "Greška pri registraciji: ${e.message ?: "Nepoznata greška"}"
                }
                onComplete(false)
            }
        }
    }

    fun signIn(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user != null) {
                    usersCollection.document(user.uid)
                        .update("lastLogin", System.currentTimeMillis())
                        .await()

                    fetchUserData(user.uid)
                    _isLoading.value = false
                    onComplete(true)
                } else {
                    _errorMessage.value = "Greška pri prijavi"
                    _isLoading.value = false
                    onComplete(false)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = when {
                    e.message?.contains("invalid credential") == true ->
                        "Pogrešan email ili lozinka"
                    e.message?.contains("user not found") == true ->
                        "Korisnik sa ovim email-om ne postoji"
                    e.message?.contains("network error") == true ->
                        "Problem sa mrežom. Proverite internet konekciju"
                    else -> "Greška pri prijavi: ${e.message}"
                }
                onComplete(false)
            }
        }
    }

    private suspend fun fetchUserData(userId: String) {
        try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                _userData.value = user
                Log.d("AuthViewModel", "User data fetched: ${user?.username}")
            } else {
                Log.d("AuthViewModel", "User document does not exist for: $userId")
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error fetching user data: ${e.message}")
        }
    }

    fun updateUserProfile(
        fullName: String? = null,
        phoneNumber: String? = null,
        profileImageBase64: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val currentUser = _currentUser.value
                if (currentUser == null) {
                    _errorMessage.value = "Korisnik nije prijavljen"
                    onComplete(false)
                    return@launch
                }

                fullName?.let { name ->
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    currentUser.updateProfile(profileUpdates).await()
                }

                val updates = mutableMapOf<String, Any>()
                fullName?.let { updates["fullName"] = it }
                phoneNumber?.let { updates["phoneNumber"] = it }
                profileImageBase64?.let { updates["profileImageBase64"] = it }

                usersCollection.document(currentUser.uid).update(updates).await()

                val currentUserData = _userData.value
                if (currentUserData != null) {
                    _userData.value = currentUserData.copy(
                        fullName = fullName ?: currentUserData.fullName,
                        phoneNumber = phoneNumber ?: currentUserData.phoneNumber,
                        profileImageBase64 = profileImageBase64 ?: currentUserData.profileImageBase64
                    )
                }

                _isLoading.value = false
                onComplete(true)

            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Greška pri ažuriranju profila: ${e.message}"
                onComplete(false)
            }
        }
    }
    fun signOut() {
        firebaseAuth.signOut()
        _userData.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}