package com.example.rmas_uross.ui.pages.objectdetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.rmas_uross.data.model.AppObject
import com.example.rmas_uross.data.model.Interaction
import com.example.rmas_uross.data.repository.ObjectRepository
import com.example.rmas_uross.ui.components.BrandTopBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetailsScreen(
    navController : NavController,
    objectId: String,
    onBack: () -> Unit,
    repository: ObjectRepository,
    currentUserId: String
) {
    var objectData by remember { mutableStateOf<AppObject?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRatingDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(objectId) {
        coroutineScope.launch {
            try {
                val result = repository.getObjectById(objectId)
                if (result.isSuccess) {
                    objectData = result.getOrNull()
                } else {
                    error = "Objekat nije pronađen"
                }
            } catch (e: Exception) {
                error = "Greška pri učitavanju: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    if (showRatingDialog && objectData != null) {
        RatingDialog(
            objectName = objectData!!.name,
            onConfirm = { rating, state, comment ->
                coroutineScope.launch {
                    val updatedObject = saveRating(
                        repository = repository,
                        appObject = objectData!!,
                        rating = rating,
                        state = state,
                        comment = comment,
                        currentUserId = currentUserId
                    )

                    if (updatedObject != null) {
                        objectData = updatedObject
                        snackbarHostState.showSnackbar(
                            message = "Hvala! Osvojili ste 20 poena za prijavu stanja.",
                            withDismissAction = true
                        )
                    }
                }
                showRatingDialog = false
            },
            onDismiss = { showRatingDialog = false }
        )
    }

    if (showReportDialog && objectData != null) {
        ReportDialog(
            objectName = objectData!!.name,
            onConfirm = { problemType, description ->
                coroutineScope.launch {
                    saveReport(
                        repository = repository,
                        appObject = objectData!!,
                        problemType = problemType,
                        description = description,
                        currentUserId = currentUserId
                    )
                    snackbarHostState.showSnackbar(
                        message = "Problem '$problemType' je uspešno zabeležen. Osvojili ste 30 poena!",
                        withDismissAction = true
                    )
                }
                showReportDialog = false
            },
            onDismiss = { showReportDialog = false }
        )
    }

    Scaffold(
        topBar = {
            BrandTopBar(
                appName = "Detalji objekta",
                showBack = true,
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Greška",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = { onBack() }) {
                                Text("Nazad")
                            }
                        }
                    }
                }
                objectData != null -> {
                    ObjectDetailsContent(
                        appObject = objectData!!,
                        navController = navController,
                        onRateClick = { showRatingDialog = true },
                        onReportClick = { showReportDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
fun ObjectDetailsContent(
    appObject: AppObject,
    navController: NavController,
    onRateClick: () -> Unit,
    onReportClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (appObject.imageUrl.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = appObject.imageUrl),
                        contentDescription = "Slika objekta",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Nema slike",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = appObject.name.ifEmpty { "Bez naziva" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = getColorByType(appObject.type)
                    )

                    InfoRow(
                        icon = Icons.Default.DataObject,
                        label = "Tip objekta",
                        value = getTypeDisplayName(appObject.type)
                    )

                    if (appObject.state.isNotBlank()) {
                        InfoRow(
                            icon = Icons.Default.Warning,
                            label = "Stanje",
                            value = appObject.state
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Lokacija",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    InfoRow(
                        icon = Icons.Default.LocationOn,
                        label = "Geografska širina",
                        value = "%.6f".format(appObject.latitude)
                    )

                    InfoRow(
                        icon = Icons.Default.LocationOn,
                        label = "Geografska dužina",
                        value = "%.6f".format(appObject.longitude)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Informacije",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    InfoRow(
                        icon = Icons.Default.Schedule,
                        label = "Dodato",
                        value = formatTimestamp(appObject.timestamp)
                    )
                    val lastUpdatedTime = appObject.lastUpdatedTimestamp
                        .takeIf { it > 0 } ?: appObject.timestamp

                    InfoRow(
                        icon = Icons.Default.Update,
                        label = "Zadnje ažurirano",
                        value = formatTimestamp(lastUpdatedTime)
                    )
                    InfoRow(
                        icon = Icons.Default.Info,
                        label = "ID objekta",
                        value = appObject.id.take(8) + "..."
                    )

                    if (appObject.userId.isNotBlank()) {
                        InfoRow(
                            icon = Icons.Default.Person,
                            label = "Dodao korisnik",
                            value = "ID: ${appObject.userId.take(8)}..."
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRateClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Prijavi stanje")
                }

                OutlinedButton(
                    onClick = onReportClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Report, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Prijavi problem")
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun RatingDialog(
    objectName: String,
    onConfirm: (Int, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRating by remember { mutableStateOf(5) }
    var selectedState by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Oceni $objectName")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Ocena:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 1..5) {
                            IconButton(onClick = { selectedRating = i }) {
                                Icon(
                                    imageVector = if (i <= selectedRating) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "$i zvezdica",
                                    tint = if (i <= selectedRating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Column {
                    Text("Stanje objekta:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val states = listOf(
                        "Ispravno" to Icons.Default.CheckCircle,
                        "Delimično oštećeno" to Icons.Default.Build,
                        "Neupotrebljivo" to Icons.Default.Warning
                    )
                    states.forEach { (state, icon) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedState == state,
                                onClick = { selectedState = state }
                            )
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state)
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Komentar (opciono)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedState.isNotEmpty()) {
                        onConfirm(selectedRating, selectedState, comment)
                    }
                },
                enabled = selectedState.isNotEmpty()
            ) {
                Text("Potvrdi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Otkaži")
            }
        }
    )
}

@Composable
fun ReportDialog(
    objectName: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProblem by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Prijavi problem.")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Tip problema:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val problems = listOf(
                        "Neprimeren sadržaj" to Icons.Default.Flag,
                        "Netačna lokacija" to Icons.Default.WrongLocation,
                        "Netačni podaci" to Icons.Default.BugReport,
                        "Drugi problem" to Icons.Default.Help
                    )
                    problems.forEach { (problem, icon) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedProblem == problem,
                                onClick = { selectedProblem = problem }
                            )
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(problem)
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis problema *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 4,
                    isError = description.isBlank()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedProblem.isNotEmpty() && description.isNotBlank()) {
                        onConfirm(selectedProblem, description)
                    }
                },
                enabled = selectedProblem.isNotEmpty() && description.isNotBlank()
            ) {
                Text("Prijavi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Otkaži")
            }
        }
    )
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getTypeDisplayName(type: String): String {
    return when (type.uppercase()) {
        "BENCH" -> "Klupica"
        "FOUNTAIN" -> "Česma"
        "OTHER" -> "Ostalo"
        else -> type
    }
}

@Composable
private fun getColorByType(type: String): Color {
    return when (type.uppercase()) {
        "BENCH" -> Color(0xFF8B4513) // braon
        "FOUNTAIN" -> Color(0xFF1976D2) // plava
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd.MM.yyyy. 'u' HH:mm", Locale.getDefault())
    return formatter.format(date)
}

private suspend fun saveRating(
    repository: ObjectRepository,
    appObject: AppObject,
    rating: Int,
    state: String,
    comment: String,
    currentUserId: String
): AppObject? {
    val interaction = Interaction(
        id = "${appObject.id}_${System.currentTimeMillis()}",
        objectId = appObject.id,
        userId = currentUserId,
        type = Interaction.TYPE_RATING,
        rating = rating,
        state = state,
        comment = comment,
        timestamp = System.currentTimeMillis(),
        pointsAwarded = 20
    )

    val result = repository.addInteraction(interaction)

    if (result.isSuccess) {
        return try {
            val currentTime = System.currentTimeMillis()
            val updatedObject = appObject.copy(
                state = state,
                lastUpdatedTimestamp = currentTime
            )
            repository.updateObject(updatedObject)
            updatedObject
        } catch (e: Exception) {
            println("Greška pri ažuriranju objekta: ${e.message}")
            appObject
        }
    } else {
        println("Greška pri čuvanju interakcije: ${result.exceptionOrNull()?.message}")
        return appObject
    }
}

private suspend fun saveReport(
    repository: ObjectRepository,
    appObject: AppObject,
    problemType: String,
    description: String,
    currentUserId: String
) {
    val interaction = Interaction(
        id = "${appObject.id}_report_${System.currentTimeMillis()}",
        objectId = appObject.id,
        userId = currentUserId,
        type = Interaction.TYPE_REPORT,
        problemType = problemType,
        comment = "Problem: $problemType\nOpis: $description",
        timestamp = System.currentTimeMillis(),
        pointsAwarded = 30
    )

    val result = repository.addInteraction(interaction)
    if (result.isFailure) {
        println("Greška pri čuvanju prijave: ${result.exceptionOrNull()?.message}")
    }
}