package com.example.rmas_uross.data.model

data class ProximityAlert(
    val id: String = "",
    val userId: String = "",
    val objectId: String = "",
    val objectName: String = "",
    val objectType: String = "",
    val radius: Int = 100,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)