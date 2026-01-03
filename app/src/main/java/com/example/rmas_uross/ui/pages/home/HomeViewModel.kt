package com.example.rmas_uross.ui.pages.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    init {
        loadUserName()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            val user = firebaseAuth.currentUser
            if (user != null) {
                val name = user.displayName
                    ?: user.email?.split("@")?.first()
                    ?: "Korisnice"
                _userName.value = "Dobrodosao, $name"
            } else {
                _userName.value = ""
            }
        }
    }
}