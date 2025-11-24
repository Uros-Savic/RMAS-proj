package com.example.rmas_uross.data.model

data class MapFilters(
    val objectTypes: List<String> = emptyList(),
    val states: List<String> = emptyList(),
    val userLat: Double? = null,
    val userLng: Double? = null,
    val radiusMeters: Double = 0.0,
    val showOnlyRated: Boolean = false,
    val minRating: Int = 0,
    val searchQuery: String = ""
) {
    fun isActive(): Boolean {
        return objectTypes.isNotEmpty() ||
                states.isNotEmpty() ||
                radiusMeters > 0 ||
                showOnlyRated ||
                minRating > 0 ||
                searchQuery.isNotBlank()
    }
}