package com.example.rmas_uross.data.model

data class Interaction(
    val id: String = "",
    val objectId: String = "",
    val userId: String = "",
    val type: String = "",
    val rating: Int = 0,
    val state: String = "",
    val problemType: String = "",
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val pointsAwarded: Int = 0
) {
    companion object {
        const val TYPE_RATING = "RATING"
        const val TYPE_REPORT = "REPORT"
    }
}