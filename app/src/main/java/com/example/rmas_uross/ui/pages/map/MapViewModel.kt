package com.example.rmas_uross.ui.pages.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rmas_uross.data.model.AppObject
import com.example.rmas_uross.data.model.MapFilters
import com.example.rmas_uross.data.repository.ObjectRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.text.contains

class MapViewModel(
    private val objectRepository: ObjectRepository
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
                _error.value = "Greška pri učitavanju objekata: ${e.message}"
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
        println("DEBUG: User location updated to ($lat, $lng)")
    }

    fun applyFilters(newFilters: MapFilters) {
        _filters.value = newFilters
        applyCurrentFilters()
    }

    private fun applyCurrentFilters() {
        val currentFilters = _filters.value
        val allObjects = _objects.value

        println("DEBUG: Applying filters - types: ${currentFilters.objectTypes}, " +
                "radius: ${currentFilters.radiusMeters}m, " +
                "location: (${currentFilters.userLat}, ${currentFilters.userLng})")

        if (!currentFilters.isActive()) {
            _filteredObjects.value = allObjects
            println("DEBUG: No active filters, showing all ${allObjects.size} objects")
            return
        }

        val filtered = allObjects.filter { objectItem ->
            val typeMatches = currentFilters.objectTypes.isEmpty() ||
                    currentFilters.objectTypes.contains(objectItem.type)

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
                if (matches) {
                    println("DEBUG: ✅ Object '${objectItem.name}' within radius: ${distance.toInt()}m <= ${currentFilters.radiusMeters.toInt()}m")
                } else {
                    println("DEBUG: ❌ Object '${objectItem.name}' outside radius: ${distance.toInt()}m > ${currentFilters.radiusMeters.toInt()}m")
                }
                matches
            } else {
                true
            }

            val searchMatches = currentFilters.searchQuery.isBlank() ||
                    objectItem.name.contains(currentFilters.searchQuery, ignoreCase = true)

            val result = typeMatches && radiusMatches && searchMatches
            result
        }

        println("DEBUG: Filter result: ${filtered.size} objects out of ${allObjects.size}")
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
        println("DEBUG: Bench filter toggled - active: ${newObjectTypes.contains("BENCH")}")
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
        println("DEBUG: Fountain filter toggled - active: ${newObjectTypes.contains("FOUNTAIN")}")
    }

    fun clearAllFilters() {
        _filters.value = MapFilters()
        _filteredObjects.value = _objects.value
        println("DEBUG: All filters cleared completely")
    }

    fun addNewMarker(position: LatLng, name: String, type: String) {
        viewModelScope.launch {
            try {
                val temporaryUserId by lazy {
                    "user_${System.currentTimeMillis()}"
                }
                val newObject = AppObject(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    type = type,
                    state = "WORKING",
                    latitude = position.latitude,
                    longitude = position.longitude,
                    description = "Novi dodat objekat",
                    timestamp = System.currentTimeMillis(),
                    userId = temporaryUserId
                )
                val result = objectRepository.addObject(newObject)
                if (result.isSuccess) {
                    println("DEBUG: New marker added successfully: $name ($type)")
                    refreshData()
                } else {
                    _error.value = "Greška pri dodavanju markera: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Greška pri dodavanju markera: ${e.message}"
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
        println("DEBUG: All filters cleared")
    }

    fun setRadiusFilter(radiusMeters: Double) {
        val currentLocation = _userLocation.value
        val newFilters = _filters.value.copy(
            radiusMeters = radiusMeters,
            userLat = currentLocation?.first,
            userLng = currentLocation?.second
        )
        applyFilters(newFilters)
        println("DEBUG: Radius filter set to ${radiusMeters.toInt()}m")
    }

    fun refreshData() {
        loadAllObjects()
    }
}