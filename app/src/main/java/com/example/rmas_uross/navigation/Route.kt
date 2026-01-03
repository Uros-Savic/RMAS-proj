package com.example.rmas_uross.navigation

object Route {
    const val login = "login"
    const val signup = "signup"
    const val home = "home"
    const val map = "map"
    const val leaderboard = "leaderboard"
    const val profile = "profile"
    const val objectDetails = "object_details/{objectId}"

    fun objectDetailsRoute(objectId: String): String = "object_details/$objectId"
}