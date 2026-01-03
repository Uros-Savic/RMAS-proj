package com.example.rmas_uross.data.model

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val profileImageBase64: String = "",
    val profileImageUrl: String = "",
    val points: Long = 0,
    val rank: String = "Novajlija",
    val objectsAdded: Int = 0,
    val reviewsWritten: Int = 0,
    val interactions: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
) {

    val level: Int
        get() = (points / 1000).toInt() + 1

    fun getExperienceForNextLevel(): Long {
        return level * 1000L
    }

    fun getProgressToNextLevel(): Float {
        val currentLevelBase = (level - 1) * 1000
        return (points - currentLevelBase).toFloat() / 1000f
    }

    fun getRankByPoints(): String {
        return when {
            points >= 10000 -> "Legenda"
            points >= 5000 -> "Master"
            points >= 2500 -> "Ekspert"
            points >= 1000 -> "Napredni"
            points >= 500 -> "Aktivni"
            points >= 100 -> "Pocetnik"
            else -> "Novajlija"
        }
    }
}
