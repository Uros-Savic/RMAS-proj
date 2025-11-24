package com.example.rmas_uross.util

object PointsSystem {
    const val POINTS_ADD_OBJECT = 50
    const val POINTS_ADD_REVIEW = 10
    const val POINTS_ADD_RATING = 5
    const val POINTS_CONFIRM_STATE = 15
    const val POINTS_DAILY_LOGIN = 5
    const val POINTS_COMPLETE_PROFILE = 25

    const val BASE_EXPERIENCE_PER_LEVEL = 1000

    fun hasUserEarnedPointsForAction(userId: String, actionType: String, targetId: String): Boolean {
        return false
    }

    fun calculatePointsForAction(actionType: String, metadata: Map<String, Any> = emptyMap()): Int {
        return when (actionType) {
            "ADD_OBJECT" -> POINTS_ADD_OBJECT
            "ADD_REVIEW" -> {
                val reviewLength = metadata["reviewLength"] as? Int ?: 0
                POINTS_ADD_REVIEW + (reviewLength / 10)
            }
            "ADD_RATING" -> POINTS_ADD_RATING
            "CONFIRM_STATE" -> POINTS_CONFIRM_STATE
            "DAILY_LOGIN" -> POINTS_DAILY_LOGIN
            "COMPLETE_PROFILE" -> POINTS_COMPLETE_PROFILE
            else -> 0
        }
    }

    fun calculateLevel(experience: Long): Int {
        var level = 1
        var expNeeded = BASE_EXPERIENCE_PER_LEVEL.toLong()
        var currentExp = experience

        while (currentExp >= expNeeded) {
            level++
            currentExp -= expNeeded
            expNeeded = (level * BASE_EXPERIENCE_PER_LEVEL).toLong()
        }

        return level
    }
}