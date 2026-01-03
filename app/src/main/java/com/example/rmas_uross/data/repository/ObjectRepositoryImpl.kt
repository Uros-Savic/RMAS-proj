package com.example.rmas_uross.data.repository

import android.net.Uri
import com.example.rmas_uross.data.model.AppObject
import com.example.rmas_uross.data.model.Interaction
import com.example.rmas_uross.data.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.*

class ObjectRepositoryImpl : ObjectRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersCollection = db.collection("users")
    private val objectsCollection = db.collection("objects")
    private val interactionsCollection = db.collection("interactions")

    override suspend fun getUser(uid: String): Result<User> {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                Result.success(document.toObject(User::class.java)!!)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePoints(uid: String, pointsToAdd: Long): Result<Unit> {
        return try {
            if (pointsToAdd <= 0) {
                return Result.success(Unit)
            }

            usersCollection.document(uid).update("points", FieldValue.increment(pointsToAdd)).await()
            usersCollection.document(uid).update("experience", FieldValue.increment(pointsToAdd)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserLocation(uid: String, latitude: Double, longitude: Double): Result<Unit> {
        return try {
            usersCollection.document(uid).update(
                "latitude", latitude,
                "longitude", longitude
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addObjectWithPoints(authorId: String, obj: AppObject, imageUri: Uri?): Result<Unit> {
        return try {
            var imageUrl = ""
            imageUri?.let { uri ->
                imageUrl = uploadImage(uri, "objects/${obj.id}")
            }
            val objectWithImage = obj.copy(imageUrl = imageUrl)
            objectsCollection.document(obj.id).set(objectWithImage).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllObjects(): Flow<List<AppObject>> = flow {
        try {
            val snapshot = objectsCollection.get().await()
            val objects = snapshot.documents.mapNotNull { document ->
                document.toObject(AppObject::class.java)
            }
            emit(objects)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getObjects(userId: String): Flow<List<AppObject>> = flow {
        try {
            val snapshot = objectsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val objects = snapshot.documents.mapNotNull { document ->
                document.toObject(AppObject::class.java)
            }
            emit(objects)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun updateObject(obj: AppObject): Result<Unit> {
        return try {
            objectsCollection.document(obj.id).set(obj).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getObjectById(objectId: String): Result<AppObject> {
        return try {
            val document = objectsCollection.document(objectId).get().await()
            if (document.exists()) {
                Result.success(document.toObject(AppObject::class.java)!!)
            } else {
                Result.failure(Exception("Object not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getObjectsInRadius(lat: Double, lon: Double, radiusKm: Double): Flow<List<AppObject>> = flow {
        try {
            val snapshot = objectsCollection.get().await()
            val allObjects = snapshot.documents.mapNotNull { document ->
                document.toObject(AppObject::class.java)
            }
            val filteredObjects = allObjects.filter { obj ->
                val distance = calculateDistance(lat, lon, obj.latitude, obj.longitude)
                distance <= radiusKm
            }

            emit(filteredObjects)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun addObject(obj: AppObject): Result<Unit> {
        return try {
            objectsCollection.document(obj.id).set(obj).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addObject(authorId: String, obj: AppObject, imageUri: Uri): Result<Unit> {
        return addObjectWithPoints(authorId, obj, imageUri)
    }

    override suspend fun addInteraction(interaction: Interaction): Result<Unit> {
        return try {
            interactionsCollection.document(interaction.id).set(interaction).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getLeaderboard(): Flow<List<User>> = flow {
        try {
            val snapshot = usersCollection
                .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            val users = snapshot.documents.mapNotNull { document ->
                document.toObject(User::class.java)
            }
            emit(users)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    private suspend fun uploadImage(uri: Uri, path: String): String {
        val storageRef = storage.reference.child("$path/${UUID.randomUUID()}")
        storageRef.putFile(uri).await()
        return storageRef.downloadUrl.await().toString()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}