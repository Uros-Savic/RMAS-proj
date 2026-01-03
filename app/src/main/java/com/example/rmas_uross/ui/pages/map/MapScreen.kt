package com.example.rmas_uross.ui.pages.map

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rmas_uross.data.model.AppObject
import com.example.rmas_uross.location.LocationService
import com.example.rmas_uross.worker.LocationTrackingService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel,
    onBack: () -> Unit,
    onObjectSelected: (String) -> Unit,
    locationService: LocationService,
    currentUserId: String = ""
) {
    val context = LocalContext.current
    val objects by viewModel.filteredObjects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filters by viewModel.filters.collectAsState()
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var showObjectsList by remember { mutableStateOf(false) }
    var isAddingMarker by remember { mutableStateOf(false) }
    var selectedPositionForMarker by remember { mutableStateOf<LatLng?>(null) }
    var showAddMarkerDialog by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var hasInitialZoomOccurred by remember { mutableStateOf(false) }
    var selectedObject by remember { mutableStateOf<AppObject?>(null) }
    val cameraPositionState = rememberCameraPositionState {
        userLocation?.let { location ->
            position = CameraPosition.fromLatLngZoom(location, 15f)
        } ?: run {
            position = CameraPosition.fromLatLngZoom(LatLng(44.7866, 20.4489), 13f)
        }
    }
    LaunchedEffect(Unit) {
        LocationTrackingService.startService(context, currentUserId)
        try {
            locationService.locationFlow.collect { location ->
                userLocation = location
                viewModel.updateUserLocation(location.latitude, location.longitude)
                locationError = null
            }
        } catch (e: Exception) {
            locationError = "Greska pri pracenju lokacije: ${e.message}"

            try {
                val currentLocation = locationService.getCurrentLocation()
                currentLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    userLocation = latLng
                    viewModel.updateUserLocation(location.latitude, location.longitude)
                }
            } catch (e2: Exception) {
                val defaultLocation = LatLng(44.7866, 20.4489)
                userLocation = defaultLocation
                viewModel.updateUserLocation(defaultLocation.latitude, defaultLocation.longitude)
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            locationService.locationFlow.collect { location ->
                userLocation = location
                viewModel.updateUserLocation(location.latitude, location.longitude)
                locationError = null
            }
        } catch (e: Exception) {
            locationError = "Greska pri pracenju lokacije: ${e.message}"

            try {
                val currentLocation = locationService.getCurrentLocation()
                currentLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    userLocation = latLng
                    viewModel.updateUserLocation(location.latitude, location.longitude)
                }
            } catch (e2: Exception) {
                val defaultLocation = LatLng(44.7866, 20.4489)
                userLocation = defaultLocation
                viewModel.updateUserLocation(defaultLocation.latitude, defaultLocation.longitude)
            }
        }
    }

    LaunchedEffect(userLocation) {
        if (userLocation != null && !hasInitialZoomOccurred) {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(userLocation!!, 15f)
                ),
                1000
            )
            hasInitialZoomOccurred = true
        }
    }

    LaunchedEffect(selectedObject) {
        selectedObject?.let { obj ->
            val position = LatLng(obj.latitude, obj.longitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(position, 16f)
                ),
                1000
            )
        }
    }

    Scaffold(
        topBar = {
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 140.dp)
            ) {
                if (filters.isActive()) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.clearAllFilters()
                            selectedObject = null
                        },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Obrisi sve filtere")
                    }
                }
                FloatingActionButton(
                    onClick = {
                        isAddingMarker = true
                        showAddMarkerDialog = true
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj marker")
                }
                FloatingActionButton(
                    onClick = { showObjectsList = !showObjectsList },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (showObjectsList) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Icon(
                        if (showObjectsList) Icons.Default.Close else Icons.Default.List,
                        contentDescription = "Lista objekata"
                    )
                }
                FloatingActionButton(
                    onClick = { showFilters = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filteri")
                }
                FloatingActionButton(
                    onClick = {
                        viewModel.toggleBenchFilter()
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = if (filters.objectTypes.contains("BENCH")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Icon(Icons.Default.Chair, contentDescription = "Klupe")
                }
                FloatingActionButton(
                    onClick = {
                        viewModel.toggleFountainFilter()
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = if (filters.objectTypes.contains("FOUNTAIN")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Icon(Icons.Default.WaterDrop, contentDescription = "Cesme")
                }
                FloatingActionButton(
                    onClick = {
                        userLocation?.let { location ->
                            cameraPositionState.position=
                                CameraPosition.fromLatLngZoom(location, 15f)
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Moja lokacija")
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapType = MapType.NORMAL
                ),
                onMapClick = { clickedPosition ->
                    if (isAddingMarker) {
                        selectedPositionForMarker = clickedPosition
                        showAddMarkerDialog = true
                        isAddingMarker = false
                    } else {
                        selectedObject = null
                    }
                },
                onMapLongClick = { longClickedPosition ->
                    selectedPositionForMarker = longClickedPosition
                    showAddMarkerDialog = true
                }
            ) {
                userLocation?.let { location ->
                    if (filters.radiusMeters > 0) {
                        Circle(
                            center = location,
                            radius = filters.radiusMeters,
                            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            strokeColor = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2f
                        )
                    }
                }
                objects.forEach { obj ->
                    val position = LatLng(obj.latitude, obj.longitude)
                    val isSelected = selectedObject?.id == obj.id

                    Marker(
                        state = MarkerState(position = position),
                        title = obj.name,
                        snippet = "${obj.getDisplayType()} • ${obj.getDisplayState()}",
                        onClick = {
                            selectedObject = obj
                            true
                        }
                    )
                    if (isSelected) {
                        Marker(
                            state = MarkerState(position = position),
                            title = "${obj.name}",
                            snippet = "Selektovano - ${obj.getDisplayType()}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)
                        )
                    }
                }
            }
            if (isAddingMarker) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Kliknite na mapu gde zelite da dodate marker",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            if (showObjectsList && objects.isNotEmpty()) {
                ObjectsListSidebar(
                    objects = objects,
                    selectedObject = selectedObject,
                    onObjectClick = { obj ->
                        selectedObject = obj
                        showObjectsList = false
                        val posi = LatLng(obj.latitude, obj.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(posi, 16f)
                    },

                    onClose = { showObjectsList = false },
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .padding(top = 72.dp,
                            start = 16.dp,
                            bottom = 16.dp)
                )
            }
            if (showFilters) {
                FilterPanel(
                    filters = filters,
                    onFiltersChanged = { newFilters ->
                        if (newFilters.radiusMeters != filters.radiusMeters) {
                            viewModel.setRadiusFilter(newFilters.radiusMeters)
                        } else {
                            viewModel.applyFilters(newFilters)
                        }
                    },
                    onClose = { showFilters = false },
                    onClear = { viewModel.clearFilters() }
                )
            }

            if (showAddMarkerDialog && selectedPositionForMarker != null) {
                AddMarkerDialog(
                    position = selectedPositionForMarker!!,
                    onConfirm = { position, name, type ->
                        viewModel.addNewMarker(position, name, type)
                        showAddMarkerDialog = false
                        selectedPositionForMarker = null
                    },
                    onDismiss = {
                        showAddMarkerDialog = false
                        selectedPositionForMarker = null
                        isAddingMarker = false
                    }
                )
            }
            locationError?.let { error ->
                Text(
                    text = error,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = buildString {
                    append("Objekti: ${objects.size} | Lokacija je aktivna.")
                    if (filters.objectTypes.isNotEmpty()) {
                        append(" • Filter: ")
                        filters.objectTypes.forEach { type ->
                            when (type) {
                                "BENCH" -> append("Klupe ")
                                "FOUNTAIN" -> append("Cesme ")
                                else -> append("$type ")
                            }
                        }
                    }
                    if (filters.radiusMeters > 0) {
                        append(" • Radijus: ${filters.radiusMeters.toInt()}m")
                    }
                    if (filters.searchQuery.isNotBlank()) {
                        append(" • Pretraga")
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, bottom = 16.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall
            )
            if (selectedObject != null) {
                SelectedObjectCard(
                    appObject = selectedObject!!,
                    onDetailsClick = {
                        onObjectSelected(selectedObject!!.id)
                    },
                    onClose = { selectedObject = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 16.dp,
                            bottom = 75.dp
                        )
                        .width(230.dp)
                )
            }
        }
    }
}

@Composable
fun ObjectsListSidebar(
    objects: List<AppObject>,
    selectedObject: AppObject?,
    onObjectClick: (AppObject) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Objekti (${objects.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Zatvori")
                }
            }
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(objects) { obj ->
                    ObjectListItem(
                        appObject = obj,
                        isSelected = selectedObject?.id == obj.id,
                        onClick = { onObjectClick(obj) }
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectListItem(
    appObject: AppObject,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 2.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when (appObject.type) {
                            "BENCH" -> Color(0xFFFF9800)
                            "FOUNTAIN" -> Color(0xFF2196F3)
                            else -> Color(0xFF9E9E9E)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (appObject.type) {
                        "BENCH" -> Icons.Default.Chair
                        "FOUNTAIN" -> Icons.Default.WaterDrop
                        else -> Icons.Default.Place
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appObject.name.ifEmpty { "Bez naziva" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = appObject.getDisplayType(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = appObject.getDisplayState(),
                    style = MaterialTheme.typography.bodySmall,
                    color = when (appObject.type) {
                        "WORKING" -> Color(0xFF388E3C)
                        "BROKEN" -> Color(0xFFD32F2F)
                        "DAMAGED" -> Color(0xFFFFA000)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selektovano",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Pogledaj detalje",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SelectedObjectCard(
    appObject: AppObject,
    onDetailsClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${appObject.name.ifEmpty { "Bez naziva" }}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Zatvori")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = when (appObject.type) {
                                "BENCH" -> Color(0xFFFF9800)
                                "FOUNTAIN" -> Color(0xFF2196F3)
                                else -> Color(0xFF9E9E9E)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (appObject.type) {
                            "BENCH" -> Icons.Default.Chair
                            "FOUNTAIN" -> Icons.Default.WaterDrop
                            else -> Icons.Default.Place
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = appObject.getDisplayType(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Lokacija: ${String.format("%.6f", appObject.latitude)}, ${String.format("%.6f", appObject.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDetailsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vidi detalje")
            }
        }
    }
}

@Composable
fun FilterPanel(
    filters: com.example.rmas_uross.data.model.MapFilters,
    onFiltersChanged: (com.example.rmas_uross.data.model.MapFilters) -> Unit,
    onClose: () -> Unit,
    onClear: () -> Unit
) {
    var selectedTypes by remember { mutableStateOf(filters.objectTypes) }
    var selectedStates by remember { mutableStateOf(filters.states) }
    var radius by remember { mutableStateOf(filters.radiusMeters) }
    var showOnlyRated by remember { mutableStateOf(filters.showOnlyRated) }
    var minRating by remember { mutableStateOf(filters.minRating) }
    var searchQuery by remember { mutableStateOf(filters.searchQuery) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Filteri mape") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Pretraga po nazivu ili opisu") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Tipovi objekata:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val objectTypes = listOf("BENCH", "FOUNTAIN")
                objectTypes.forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedTypes.contains(type),
                            onCheckedChange = { checked ->
                                selectedTypes = if (checked) {
                                    selectedTypes + type
                                } else {
                                    selectedTypes - type
                                }
                            }
                        )
                        Text(
                            when (type) {
                                "BENCH" -> "Klupe"
                                "FOUNTAIN" -> "Cesme"
                                else -> type
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Stanje objekata:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val states = listOf("WORKING", "DAMAGED", "BROKEN")
                states.forEach { state ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedStates.contains(state),
                            onCheckedChange = { checked ->
                                selectedStates = if (checked) {
                                    selectedStates + state
                                } else {
                                    selectedStates - state
                                }
                            }
                        )
                        Text(
                            when (state) {
                                "WORKING" -> "Ispravno"
                                "DAMAGED" -> "Delimicno osteceno"
                                "BROKEN" -> "Neupotrebljivo"
                                else -> state
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Radijus pretrage: ${radius.toInt()}m",
                    style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = radius.toFloat(),
                    onValueChange = { radius = it.toDouble() },
                    valueRange = 0f..5000f,
                    steps = 49
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val newFilters = com.example.rmas_uross.data.model.MapFilters(
                    objectTypes = selectedTypes,
                    states = selectedStates,
                    radiusMeters = radius,
                    showOnlyRated = showOnlyRated,
                    minRating = minRating,
                    searchQuery = searchQuery
                )
                onFiltersChanged(newFilters)
                onClose()
            }) {
                Text("Primeni")
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onClear) {
                    Text("Obrisi sve filtere")
                }
                TextButton(onClick = onClose) {
                    Text("Zatvori")
                }
            }
        }
    )
}

@Composable
fun AddMarkerDialog(
    position: LatLng,
    onConfirm: (LatLng, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var markerName by remember { mutableStateOf("") }
    var markerType by remember { mutableStateOf("") }
    val markerTypes = listOf("BENCH", "FOUNTAIN")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj novi marker") },
        text = {
            Column {
                Text(
                    text = "Pozicija: ${String.format("%.6f", position.latitude)}, ${String.format("%.6f", position.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = markerName,
                    onValueChange = { markerName = it },
                    label = { Text("Naziv markera") },
                    placeholder = { Text("Npr. Klupa u parku") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Tip objekta:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                markerTypes.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { markerType = type }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = markerType == type,
                            onClick = { markerType = type }
                        )
                        Text(
                            text = when (type) {
                                "BENCH" -> "Klupa"
                                "FOUNTAIN" -> "Cesma"
                                else -> type
                            },
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(position, markerName, markerType)
                },
                enabled = markerName.isNotEmpty() && markerType.isNotEmpty()
            ) {
                Text("Dodaj marker")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Otkazi")
            }
        }
    )
}
