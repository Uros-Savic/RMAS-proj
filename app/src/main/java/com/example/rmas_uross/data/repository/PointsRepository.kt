package com.example.rmas_uross.data.repository

import com.example.rmas_uross.data.model.Interaction
import com.example.rmas_uross.data.model.User
import com.example.rmas_uross.util.PointsSystem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
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
            if (hasUserEarnedPoints(userId, actionType, targetId)) {
                return Result.success(0)
            }
            val points = PointsSystem.calculatePointsForAction(actionType, metadata)
            val userDoc = firestore.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java)

            user?.let {
                val newPoints = it.points + points
                val newExperience = it.experience + points
                val newLevel = PointsSystem.calculateLevel(newExperience)
                val updates = mutableMapOf<String, Any>(
                    "points" to newPoints,
                    "experience" to newExperience,
                    "level" to newLevel,
                    "lastActivity" to System.currentTimeMillis()
                )
                when (actionType) {
                    "ADD_OBJECT" -> updates["objectsAdded"] = it.objectsAdded + 1
                    "ADD_REVIEW" -> updates["reviewsWritten"] = it.reviewsWritten + 1
                }
                updates["rank"] = getRankByPoints(newPoints)
                updates["interactions"] = it.interactions + 1

                firestore.collection("users").document(userId).update(updates).await()
            }
            recordInteraction(
                Interaction(
                    userId = userId,
                    objectId = targetId,
                    type = actionType,
                    pointsAwarded = points,
                    timestamp = System.currentTimeMillis()
                )
            )

            Result.success(points)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun hasUserEarnedPoints(userId: String, actionType: String, targetId: String): Boolean {
        return try {
            val snapshot = firestore.collection("interactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", actionType)
                .whereEqualTo("objectId", targetId)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun recordInteraction(interaction: Interaction) {
        try {
            firestore.collection("interactions").add(interaction).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getUserStats(userId: String): User? {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            document.toObject(User::class.java)
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

            snapshot.documents.mapNotNull { document ->
                val user = document.toObject(User::class.java)
                user?.copy(uid = document.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getLeaderboardFlow(limit: Int = 50): Flow<List<User>> = flow {
        try {
            val snapshot = firestore.collection("users")
                .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { document ->
                val user = document.toObject(User::class.java)
                user?.copy(uid = document.id)
            }
            emit(users)
        } catch (e: Exception) {
            emit(emptyList())
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
            points >= 2500 -> "Master"
            points >= 1000 -> "Ekspert"
            points >= 500 -> "Napredni"
            points >= 100 -> "Početnik"
            else -> "Novajlija"
        }
    }
}