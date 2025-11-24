package com.example.rmas_uross.data.repository

import com.example.rmas_uross.data.model.Notification
import com.example.rmas_uross.data.model.ProximityAlert
import com.example.rmas_uross.data.model.MapFilters
import com.example.rmas_uross.data.model.AppObject
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.math.*

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")
    private val proximityAlertsCollection = db.collection("proximity_alerts")
    private val objectsCollection = db.collection("objects")
    suspend fun getNearbyObjects(
        userLat: Double,
        userLng: Double,
        radiusMeters: Double,
        objectTypes: List<String> = emptyList()
    ): List<AppObject> {
        return try {
            val allObjects = objectsCollection.get().await()
                .toObjects(AppObject::class.java)
            allObjects.filter { obj ->
                val distance = calculateDistance(
                    userLat, userLng,
                    obj.latitude, obj.longitude
                )

                val isInRadius = distance <= radiusMeters
                val matchesType = objectTypes.isEmpty() || objectTypes.contains(obj.type)

                isInRadius && matchesType
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun createProximityAlert(alert: ProximityAlert): Boolean {
        return try {
            proximityAlertsCollection.document(alert.id).set(alert).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    suspend fun getUserProximityAlerts(userId: String): List<ProximityAlert> {
        return try {
            proximityAlertsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(ProximityAlert::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteProximityAlert(alertId: String): Boolean {
        return try {
            proximityAlertsCollection.document(alertId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkProximityAlerts(
        userLat: Double,
        userLng: Double,
        userId: String
    ): List<AppObject> {
        return try {
            val userAlerts = getUserProximityAlerts(userId)
            val nearbyObjects = getNearbyObjects(userLat, userLng, 100.0)

            val triggeredObjects = mutableListOf<AppObject>()

            userAlerts.forEach { alert ->
                val targetObject = nearbyObjects.find { it.id == alert.objectId }
                if (targetObject != null) {
                    triggeredObjects.add(targetObject)
                    createNotification(
                        Notification(
                            userId = userId,
                            title = "Objekat u blizini!",
                            message = "${targetObject.name} je na ${alert.radius}m od vas",
                            type = "PROXIMITY_ALERT",
                            relatedObjectId = targetObject.id
                        )
                    )
                }
            }
            triggeredObjects
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun getObjectsWithFilters(
        filters: MapFilters
    ): List<AppObject> {
        return try {
            var query: Query = objectsCollection
            if (filters.objectTypes.isNotEmpty()) {
                query = query.whereIn("type", filters.objectTypes)
            }
            if (filters.states.isNotEmpty()) {
                query = query.whereIn("state", filters.states)
            }
            val objects = query.get().await().toObjects(AppObject::class.java)

            if (filters.userLat != null && filters.userLng != null && filters.radiusMeters > 0) {
                objects.filter { obj ->
                    val distance = calculateDistance(
                        filters.userLat, filters.userLng,
                        obj.latitude, obj.longitude
                    )
                    distance <= filters.radiusMeters
                }
            } else {
                objects
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun getUserNotifications(userId: String): List<Notification> {
        return try {
            notificationsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Notification::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
    private suspend fun createNotification(notification: Notification): Boolean {
        return try {
            notificationsCollection.add(notification).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markNotificationAsRead(notificationId: String): Boolean {
        return try {
            notificationsCollection.document(notificationId).update("read", true).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAllNotificationsAsRead(userId: String): Boolean {
        return try {
            val notifications = getUserNotifications(userId)
            val unreadNotifications = notifications.filter { !it.read }

            val batch = db.batch()
            unreadNotifications.forEach { notification ->
                val ref = notificationsCollection.document(notification.id)
                batch.update(ref, "read", true)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteNotification(notificationId: String): Boolean {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}