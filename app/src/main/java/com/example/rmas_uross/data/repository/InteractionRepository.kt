package com.example.rmas_uross.data.repository

import com.example.rmas_uross.data.model.Interaction

interface InteractionRepository {
    suspend fun addInteraction(interaction: Interaction): Result<Unit>
    suspend fun hasUserInteracted(userId: String, objectId: String, type: String): Boolean
}

data class InteractionStats(
    val totalRatings: Int = 0,
    val averageRating: Double = 0.0,
    val totalReports: Int = 0,
    val totalLikes: Int = 0,
    val lastInteraction: Long = 0
)