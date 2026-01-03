package com.example.rmas_uross.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.rmas_uross.navigation.Route

data class BottomNavigationItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavigationItem(Route.home, Icons.Filled.Home, "Pocetna"),
    BottomNavigationItem(Route.map, Icons.Filled.Place, "Mapa"),
    BottomNavigationItem(Route.leaderboard, Icons.Filled.AccountBox, "Rang Lista"),
    BottomNavigationItem(Route.profile, Icons.Filled.Person, "Profil"),
)


@Composable
fun BottomBar(
    currentRoute: String,
    onHome: () -> Unit,
    onMap: () -> Unit,
    onLeaderboard: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    when (item.route) {
                        Route.home -> onHome()
                        Route.map -> onMap()
                        Route.leaderboard -> onLeaderboard()
                        Route.profile -> onProfile()
                    }
                }
            )
        }
    }
}
