package com.example.rmas_uross.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

public class LocationRepository(private val firestore: FirebaseFirestore) {

    suspend fun saveUserLocation(
            userId: String,
            latitude: Double,
            longitude: Double,
            accuracy: Float
    ) {
        val locationData = hashMapOf(
                "userId" to userId,
                "location" to GeoPoint(latitude, longitude),
                "accuracy" to accuracy,
                "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("user_locations")
                .document(userId)
                .set(locationData)
                .await()
    }
}