package com.example.rmas_uross.data.repository

import android.net.Uri
import com.example.rmas_uross.data.model.AppObject
import com.example.rmas_uross.data.model.Interaction
import com.example.rmas_uross.data.model.User
import kotlinx.coroutines.flow.Flow

interface ObjectRepository {
    suspend fun getUser(uid: String): Result<User>
    suspend fun updatePoints(uid: String, pointsToAdd: Long): Result<Unit>
    suspend fun updateUserLocation(uid: String, latitude: Double, longitude: Double): Result<Unit>
    suspend fun addObjectWithPoints(authorId: String, obj: AppObject, imageUri: Uri? = null): Result<Unit>
    fun getAllObjects(): Flow<List<AppObject>>
    fun getObjects(userId: String): Flow<List<AppObject>>
    suspend fun updateObject(obj: AppObject): Result<Unit>
    suspend fun getObjectById(objectId: String): Result<AppObject>
    fun getObjectsInRadius(lat: Double, lon: Double, radiusKm: Double): Flow<List<AppObject>>
    suspend fun addObject(obj: AppObject): Result<Unit>
    suspend fun addObject(authorId: String, obj: AppObject, imageUri: Uri): Result<Unit>
    suspend fun addInteraction(interaction: Interaction): Result<Unit>
    fun getLeaderboard(): Flow<List<User>>
}