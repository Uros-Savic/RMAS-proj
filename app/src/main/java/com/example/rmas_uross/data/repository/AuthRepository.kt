package com.example.rmas_uross.data.repository

import android.net.Uri
import com.example.rmas_uross.data.model.User

interface AuthRepository {
    suspend fun signUp(user: User, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<User>
    fun signOut()
    suspend fun uploadProfilePicture(uid: String, imageUri: Uri): Result<String>
    fun getCurrentUser(): User?
}