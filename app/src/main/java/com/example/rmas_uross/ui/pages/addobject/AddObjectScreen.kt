package com.example.rmas_uross.ui.pages.addobject

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // DODATO
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rmas_uross.location.LocationService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObjectScreen(
    navController: NavController,
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    initialObjectType: String? = null,
    locationService: LocationService,
    viewModel: AddObjectViewModel = viewModel()
) {

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var selectedType by remember {
        mutableStateOf(initialObjectType ?: "BENCH")
    }

    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: Exception) { // ..sdfgsd
            }
            viewModel.onImageUriChange(uri)
        } else {
            viewModel.onImageUriChange(null)
        }
    }
    val currentImageUri = uiState.imageUri

    LaunchedEffect(initialLatitude, initialLongitude, initialObjectType) {
        if (initialLatitude != null && initialLongitude != null) {
            viewModel.updateLocation(initialLatitude, initialLongitude)
        }
        if (initialObjectType != null) {
            viewModel.onTypeChange(initialObjectType)
            selectedType = initialObjectType
        }
    }
    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Objekat uspešno sačuvan!")
                }
                kotlinx.coroutines.delay(1500)
                navController.popBackStack()
            }
            is SaveState.Error -> {
                val errorMessage = (saveState as SaveState.Error).message
                scope.launch {
                    snackbarHostState.showSnackbar(errorMessage)
                }
            }
            else -> {}
        }
    }

    val objectTypeDisplayName = when (selectedType) {
        "BENCH" -> "Klupica"
        "FOUNTAIN" -> "Česma"
        "OTHER" -> "Ostalo"
        else -> selectedType
    }
    val isLoading = saveState is SaveState.Loading
    val isFormValid = name.isNotBlank() &&
            uiState.latitude != null &&
            uiState.longitude != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            initialObjectType != null -> "Dodaj $objectTypeDisplayName"
                            else -> "Kreiraj objekat"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    viewModel.onNameChange(name)
                    viewModel.onDescriptionChange(description)
                    viewModel.onTypeChange(selectedType)
                    viewModel.onStateChange(state)
                    viewModel.saveObject()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = isFormValid && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(16.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Čuvanje...")
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Text("Sačuvaj", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Kreiranje objekta",
                style = MaterialTheme.typography.headlineSmall
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "• Tip: $objectTypeDisplayName",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (uiState.latitude != null && uiState.longitude != null) {
                    Text(
                        text = "• Lokacija: ${"%.6f".format(uiState.latitude)}, ${"%.6f".format(uiState.longitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "• Lokacija: Nije postavljena",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Naziv *") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Unesite naziv objekta") },
                    isError = name.isBlank(),
                    supportingText = {
                        if (name.isBlank()) {
                            Text("Naziv je obavezan")
                        }
                    },
                    enabled = !isLoading
                )

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Unesite opis objekta") },
                    singleLine = false,
                    enabled = !isLoading
                )

                TextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("Stanje") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Npr. dobro, oštećeno, novo...") },
                    enabled = !isLoading
                )

                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val buttonText = if (currentImageUri != null && currentImageUri != Uri.EMPTY) {
                        "Slika odabrana"
                    } else {
                        "Odaberi sliku (opciono)"
                    }
                    Text(buttonText)
                }
            }

            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Čuvanje objekta...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}