package com.example.rmas_uross.data.repository

import com.example.rmas_uross.data.model.Interaction
import com.example.rmas_uross.data.model.User
import com.example.rmas_uross.util.PointsSystem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PointsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    constructor() : this(Firebase.firestore)

    suspend fun awardPoints(
        userId: String,
        actionType: String,
        targetId: String,
        metadata: Map<String, Any> = emptyMap()
    ): Result<Int> {
        return try {


            if (actionType != "ADD_OBJECT" && hasUserEarnedPoints(userId, actionType, targetId)) {
                return Result.success(0)
            }
            val points = PointsSystem.calculatePointsForAction(actionType, metadata)

            if (points <= 0) {
                return Result.success(0)
            }
            try {
                val userRef = firestore.collection("users").document(userId)

                val userDoc = userRef.get().await()
                if (!userDoc.exists()) {
                    userRef.set(
                        mapOf(
                            "points" to 0L,
                            "objectsAdded" to 0L,
                            "interactions" to 0L,
                            "reviewsWritten" to 0L,
                            "confirmations" to 0L,
                            "lastActivity" to System.currentTimeMillis()
                        )
                    ).await()
                }
                val updateData = mutableMapOf<String, Any>(
                    "points" to FieldValue.increment(points.toLong()),
                    "lastActivity" to System.currentTimeMillis(),
                    "interactions" to FieldValue.increment(1)
                )

                when (actionType) {
                    "ADD_OBJECT" -> {
                        updateData["objectsAdded"] = FieldValue.increment(1)
                    }
                    "ADD_REVIEW", "ADD_RATING" -> {
                        updateData["reviewsWritten"] = FieldValue.increment(1)
                    }
                    "CONFIRM_STATE" -> {
                        updateData["confirmations"] = FieldValue.increment(1)
                    }
                }

                userRef.update(updateData).await()

            } catch (e: Exception) {
                throw e
            }

            try {
                val interaction = Interaction(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    objectId = targetId,
                    type = actionType,
                    pointsAwarded = points,
                    timestamp = System.currentTimeMillis()
                )

                firestore.collection("interactions")
                    .document(interaction.id)
                    .set(interaction)
                    .await()

            } catch (e: Exception) {
            }

            try {
                updateUserRank(userId)
            } catch (e: Exception) {
            }

            Result.success(points)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun hasUserEarnedPoints(userId: String, actionType: String, targetId: String): Boolean {
        return try {
            if (actionType == "ADD_OBJECT") {
                return false
            }

            val snapshot = firestore.collection("interactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", actionType)
                .whereEqualTo("objectId", targetId)
                .limit(1)
                .get()
                .await()

            val alreadyEarned = !snapshot.isEmpty

            alreadyEarned
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserStats(userId: String): User? {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            val user = document.toObject(User::class.java)
            if (user != null) {
            }
            user
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLeaderboard(limit: Int = 50): List<User> {
        return try {
            val snapshot = firestore.collection("users")
                .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { document ->
                val user = document.toObject(User::class.java)
                user?.copy(uid = document.id)
            }

            users
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateUserRank(userId: String) {
        try {
            val user = getUserStats(userId)
            user?.let {
                val rank = getRankByPoints(it.points)
                firestore.collection("users").document(userId).update("rank", rank).await()
            }
        } catch (e: Exception) {
        }
    }

    private fun getRankByPoints(points: Long): String {
        return when {
            points >= 5000 -> "Legenda"
            points >= 4000 -> "Master"
            points >= 3000 -> "Ekspert"
            points >= 2000 -> "Napredni"
            points >= 1000 -> "Pocetnik"
            else -> "Novajlija"
        }
    }
}