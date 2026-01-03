package com.example.rmas_uross.data.model

data class AppObject(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val state: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val userId: String = "",
    val timestamp: Long = 0L,
    val lastUpdatedTimestamp: Long = 0L,
    val imageUrl: String = "",
    val imageBase64: String = "",
    val rating: Double = 0.0,
    val ratingCount: Int = 0
) {
    companion object {
        const val TYPE_BENCH = "BENCH"
        const val TYPE_FOUNTAIN = "FOUNTAIN"

        const val STATE_WORKING = "WORKING"
        const val STATE_DAMAGED = "DAMAGED"
        const val STATE_BROKEN = "BROKEN"
    }

    fun getDisplayType(): String {
        return when (type) {
            TYPE_BENCH -> "Klupa"
            TYPE_FOUNTAIN -> "Cesma"
            else -> type
        }
    }

    fun getDisplayState(): String {
        return when (state) {
            STATE_WORKING -> "Ispravno"
            STATE_DAMAGED -> "Delimicno osteceno"
            STATE_BROKEN -> "Neupotrebljivo"
            else -> return this.state
        }
    }
}