package com.example.rmas_uross.ui.pages.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rmas_uross.data.model.AppObject
import com.example.rmas_uross.data.model.MapFilters
import com.example.rmas_uross.data.repository.ObjectRepository
import com.example.rmas_uross.data.repository.PointsRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.text.contains

class MapViewModel(
    private val objectRepository: ObjectRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val pointsRepository: PointsRepository = PointsRepository()
) : ViewModel() {

    private val _objects = MutableStateFlow<List<AppObject>>(emptyList())
    val objects: StateFlow<List<AppObject>> = _objects.asStateFlow()

    private val _filteredObjects = MutableStateFlow<List<AppObject>>(emptyList())
    val filteredObjects: StateFlow<List<AppObject>> = _filteredObjects.asStateFlow()

    private val _filters = MutableStateFlow(MapFilters())
    val filters: StateFlow<MapFilters> = _filters.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAllObjects()
    }

    fun loadAllObjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                objectRepository.getAllObjects().collect { objectsList ->
                    _objects.value = objectsList
                    applyCurrentFilters()
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _objects.value = emptyList()
                _filteredObjects.value = emptyList()
                _error.value = "Greska pri ucitavanju objekata: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun updateUserLocation(lat: Double, lng: Double) {
        _userLocation.value = Pair(lat, lng)

        val currentFilters = _filters.value
        if (currentFilters.radiusMeters > 0) {
            val newFilters = currentFilters.copy(
                userLat = lat,
                userLng = lng
            )
            applyFilters(newFilters)
        } else {
            _filters.value = currentFilters.copy(
                userLat = lat,
                userLng = lng
            )
        }
    }

    fun applyFilters(newFilters: MapFilters) {
        _filters.value = newFilters
        applyCurrentFilters()
    }

    private fun applyCurrentFilters() {
        val currentFilters = _filters.value
        val allObjects = _objects.value
        if (!currentFilters.isActive()) {
            _filteredObjects.value = allObjects
            return
        }

        val filtered = allObjects.filter { objectItem ->
            val typeMatches = currentFilters.objectTypes.isEmpty() ||
                    currentFilters.objectTypes.contains(objectItem.type)
            val stateMatches = currentFilters.states.isEmpty() ||
                    currentFilters.states.contains(objectItem.state)
            val radiusMatches = if (currentFilters.userLat != null &&
                currentFilters.userLng != null &&
                currentFilters.radiusMeters > 0) {
                val distance = calculateDistance(
                    currentFilters.userLat,
                    currentFilters.userLng,
                    objectItem.latitude,
                    objectItem.longitude
                )
                val matches = distance <= currentFilters.radiusMeters

                matches
            } else {
                true
            }

            val searchMatches = currentFilters.searchQuery.isBlank() ||
                    objectItem.name.contains(currentFilters.searchQuery, ignoreCase = true)

            val result = typeMatches && stateMatches && radiusMatches && searchMatches
            result
        }

        _filteredObjects.value = filtered
    }

    fun toggleBenchFilter() {
        val currentFilters = _filters.value
        val newObjectTypes = if (currentFilters.objectTypes.contains("BENCH")) {
            currentFilters.objectTypes - "BENCH"
        } else {
            (currentFilters.objectTypes - "FOUNTAIN") + "BENCH"
        }

        val newFilters = currentFilters.copy(objectTypes = newObjectTypes)
        applyFilters(newFilters)
    }

    fun toggleFountainFilter() {
        val currentFilters = _filters.value
        val newObjectTypes = if (currentFilters.objectTypes.contains("FOUNTAIN")) {
            currentFilters.objectTypes - "FOUNTAIN"
        } else {
            (currentFilters.objectTypes - "BENCH") + "FOUNTAIN"
        }

        val newFilters = currentFilters.copy(objectTypes = newObjectTypes)
        applyFilters(newFilters)
    }

    fun clearAllFilters() {
        _filters.value = MapFilters()
        _filteredObjects.value = _objects.value
    }

    fun addNewMarker(position: LatLng, name: String, type: String) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            _error.value = "Morate biti prijavljeni da biste dodali objekat"
            return
        }

        viewModelScope.launch {
            try {
                val objectId = UUID.randomUUID().toString()

                val newObject = AppObject(
                    id = objectId,
                    name = name,
                    type = type,
                    state = "WORKING",
                    latitude = position.latitude,
                    longitude = position.longitude,
                    description = "Dodat sa mape",
                    timestamp = System.currentTimeMillis(),
                    userId = userId,
                    imageUrl = ""
                )
                val result = objectRepository.addObject(newObject)

                if (result.isFailure) {
                    val error = result.exceptionOrNull()
                    _error.value = "Greska pri dodavanju: ${error?.message}"
                    return@launch
                }

                val pointsResult = pointsRepository.awardPoints(
                    userId = userId,
                    actionType = "ADD_OBJECT",
                    targetId = objectId,
                    metadata = mapOf(
                        "type" to type,
                        "hasImage" to false
                    )
                )

                if (pointsResult.isSuccess) {
                    val awarded = pointsResult.getOrNull() ?: 0
                    _error.value = "Marker dodat! +$awarded poena"
                } else {
                    val error = pointsResult.exceptionOrNull()
                    _error.value = "Marker dodat, ali poeni nisu dodeljeni"
                }
                refreshData()

            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Greska: ${e.message}"
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }

    fun clearFilters() {
        _filters.value = MapFilters()
        _filteredObjects.value = _objects.value
    }

    fun setRadiusFilter(radiusMeters: Double) {
        val currentLocation = _userLocation.value
        val newFilters = _filters.value.copy(
            radiusMeters = radiusMeters,
            userLat = currentLocation?.first,
            userLng = currentLocation?.second
        )
        applyFilters(newFilters)
    }

    fun refreshData() {
        loadAllObjects()
    }

    override fun onCleared() {
        super.onCleared()
    }
}