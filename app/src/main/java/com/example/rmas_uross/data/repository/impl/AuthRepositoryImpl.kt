package com.example.rmas_uross.data.repository.impl

import android.net.Uri
import com.example.rmas_uross.data.model.User
import com.example.rmas_uross.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val USERS_COLLECTION = "users"

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
    }

    override suspend fun uploadProfilePicture(uid: String, imageUri: Uri): Result<String> {
        return try {
            val imageRef = storage.reference.child("profile_pictures/$uid.jpg")
            imageRef.putFile(imageUri).await()
            val imageUrl = imageRef.downloadUrl.await().toString()
            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(user: User, password: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(user.email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("UID nije dostupan nakon registracije.")
            val newUser = user.copy(uid = uid, points = 0)
            firestore.collection(USERS_COLLECTION).document(uid).set(newUser).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val uid = auth.currentUser?.uid ?: throw Exception("Neuspesna prijava.")

            val userDocument = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            val user = userDocument.toObject(User::class.java) ?: throw Exception("Korisnik nije pronadjen u bazi.")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}