package com.example.rmas_uross.ui.pages.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel)
{
    LaunchedEffect(Unit) {
        viewModel.loadLeaderboard()
    }

    val leaderboard by viewModel.leaderboard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {

        },
        floatingActionButton = {
            if (error != null || leaderboard.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.refreshLeaderboard() },
                    icon = { Icon(Icons.Default.Refresh, "Osvezi") },
                    text = { Text("Osvezi") },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Ucitavanje rang liste...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
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
                                imageVector = Icons.Default.Error,
                                contentDescription = "Greska",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(onClick = { viewModel.refreshLeaderboard() }) {
                                Text("Pokusaj ponovo")
                            }
                        }
                    }
                }
                leaderboard.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = "Prazna rang lista",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Nema podataka za rang listu",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Budite prvi koji ce dodati objekat i osvojiti poene!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        item {
                            LeaderboardStatsHeader(leaderboard)
                        }
                        items(leaderboard) { user ->
                            LeaderboardItem(
                                user = user,
                                position = leaderboard.indexOf(user) + 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardStatsHeader(leaderboard: List<LeaderboardUser>) {
    val totalUsers = leaderboard.size
    val totalPoints = leaderboard.sumOf { it.points }
    val avgPoints = if (totalUsers > 0) totalPoints / totalUsers else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Rang Lista - Statistike",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalUsers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Korisnika",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalPoints",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Ukupno poena",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$avgPoints",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Prosek",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(user: LeaderboardUser, position: Int) {
    val isTopThree = position <= 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(if (isTopThree) 8.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (position) {
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                if (isTopThree) {
                    Icon(
                        imageVector = when (position) {
                            1 -> Icons.Default.LooksOne
                            2 -> Icons.Default.LooksTwo
                            3 -> Icons.Default.Looks3
                            else -> Icons.Default.Numbers
                        },
                        contentDescription = "Pozicija $position",
                        tint = when (position) {
                            1 -> Color(0xFFD4AF37)
                            2 -> Color(0xFFA8A8A8)
                            3 -> Color(0xFFCD7F32)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Text(
                        text = "$position",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (user.profileImage.isNotEmpty()) {
                AsyncImage(
                    model = user.profileImage,
                    contentDescription = "Profilna slika ${user.username}",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nivo ${user.level}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // progreess bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = user.getProgressToNextLevel())
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${user.points}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "poena",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}