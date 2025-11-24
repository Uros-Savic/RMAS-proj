package com.example.rmas_uross.data.repository

import android.net.Uri
import com.example.rmas_uross.data.model.AppObject
import com.example.rmas_uross.data.model.Interaction
import com.example.rmas_uross.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FirebaseObjectRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ObjectRepository {

    override suspend fun getUser(uid: String): Result<User> = try {
        val snapshot = firestore.collection("users").document(uid).get().await()
        val user = snapshot.toObject(User::class.java)
        if (user != null) Result.success(user)
        else Result.failure(Exception("Korisnik ne postoji"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updatePoints(uid: String, pointsToAdd: Long): Result<Unit> = try {
        firestore.collection("users").document(uid)
            .update("points", com.google.firebase.firestore.FieldValue.increment(pointsToAdd))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateUserLocation(
        uid: String,
        latitude: Double,
        longitude: Double
    ): Result<Unit> = try {
        firestore.collection("users").document(uid)
            .update(mapOf("latitude" to latitude, "longitude" to longitude))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getAllObjects(): Flow<List<AppObject>> = callbackFlow {
        val listener = firestore.collection("objects")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(AppObject::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun getObjects(userId: String): Flow<List<AppObject>> = callbackFlow {
        val listener = firestore.collection("objects")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(AppObject::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getObjectById(objectId: String): Result<AppObject> = try {
        val doc = firestore.collection("objects").document(objectId).get().await()
        val obj = doc.toObject(AppObject::class.java)
        if (obj != null) Result.success(obj)
        else Result.failure(Exception("Objekat ne postoji"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getObjectsInRadius(
        lat: Double,
        lon: Double,
        radiusKm: Double
    ): Flow<List<AppObject>> = getAllObjects()
    override suspend fun addObject(obj: AppObject): Result<Unit> = try {
        val docRef = if (obj.id.isBlank())
            firestore.collection("objects").document()
        else
            firestore.collection("objects").document(obj.id)

        val finalObj = obj.copy(id = docRef.id)

        docRef.set(finalObj).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    override suspend fun updateObject(obj: AppObject): Result<Unit> {
        return try {
            firestore.collection("objects")
                .document(obj.id)
                .set(obj)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addObject(
        authorId: String,
        obj: AppObject,
        imageUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("objects").document()

            val imageRef = storage.reference.child(
                "objects/${docRef.id}_${System.currentTimeMillis()}.jpg"
            )

            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            val finalObj = obj.copy(
                id = docRef.id,
                imageUrl = downloadUrl,
                userId = authorId
            )

            docRef.set(finalObj).await()
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private val pointsRepository = PointsRepository()

    override suspend fun addObjectWithPoints(
        authorId: String,
        obj: AppObject,
        imageUri: Uri?
    ): Result<Unit> = try {

        val finalResult = if (imageUri != null && imageUri != Uri.EMPTY) {
            addObject(authorId, obj, imageUri)
        } else {
            addObject(obj)
        }

        if (finalResult.isSuccess) {
            pointsRepository.awardPoints(
                userId = authorId,
                actionType = "ADD_OBJECT",
                targetId = obj.id,
                metadata = mapOf("objectType" to obj.type)
            )
        }

        finalResult

    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addInteraction(interaction: Interaction): Result<Unit> {
        return try {
            val interactionWithId = if (interaction.id.isEmpty()) {
                interaction.copy(id = UUID.randomUUID().toString())
            } else {
                interaction
            }

            firestore.collection("interactions")
                .document(interactionWithId.id)
                .set(interactionWithId)
                .await()

            when (interaction.type) {
                Interaction.TYPE_RATING -> {
                    updatePoints(interaction.userId, 20)
                }
                Interaction.TYPE_REPORT -> {
                    updatePoints(interaction.userId, 30)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
     suspend fun getObjectInteractions(objectId: String): List<Interaction> {
        return try {
            val snapshot = firestore.collection("interactions")
                .whereEqualTo("objectId", objectId)
                .get()
                .await()

            snapshot.toObjects(Interaction::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

     suspend fun hasUserInteracted(userId: String, objectId: String, type: String): Boolean {
        return try {
            val snapshot = firestore.collection("interactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("objectId", objectId)
                .whereEqualTo("type", type)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
    suspend fun getUserInteractions(userId: String): List<Interaction> {
        return try {
            val snapshot = firestore.collection("interactions")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.toObjects(Interaction::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
     suspend fun getInteractionStats(objectId: String): InteractionStats {
        val interactions = getObjectInteractions(objectId)

        val ratings = interactions.filter { it.type == Interaction.TYPE_RATING && it.rating > 0 }
        val averageRating = if (ratings.isNotEmpty()) {
            ratings.map { it.rating }.average()
        } else {
            0.0
        }

        return InteractionStats(
            totalRatings = ratings.size,
            averageRating = averageRating,
            totalReports = interactions.count { it.type == Interaction.TYPE_REPORT },
            totalLikes = interactions.count { it.type == Interaction.TYPE_LIKE },
            lastInteraction = interactions.maxByOrNull { it.timestamp }?.timestamp ?: 0
        )
    }

    override fun getLeaderboard(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .orderBy("points", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(User::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }
}
