package com.example.rmas_uross.data.model

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val read: Boolean = false,
    val relatedObjectId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)