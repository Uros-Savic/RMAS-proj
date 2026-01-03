package com.example.rmas_uross

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rmas_uross.data.repository.FirebaseObjectRepository
import com.example.rmas_uross.data.repository.ImageUploadRepository
import com.example.rmas_uross.data.repository.ObjectRepository
import com.example.rmas_uross.data.repository.PointsRepository
import com.example.rmas_uross.location.LocationService
import com.example.rmas_uross.navigation.Route
import com.example.rmas_uross.ui.auth.AuthViewModel
import com.example.rmas_uross.ui.components.BottomBar
import com.example.rmas_uross.ui.pages.home.HomeScreen
import com.example.rmas_uross.ui.pages.home.HomeViewModel
import com.example.rmas_uross.ui.pages.leaderboard.LeaderboardScreen
import com.example.rmas_uross.ui.pages.leaderboard.LeaderboardViewModel
import com.example.rmas_uross.ui.pages.login.LoginScreen
import com.example.rmas_uross.ui.pages.map.MapScreen
import com.example.rmas_uross.ui.pages.map.MapViewModel
import com.example.rmas_uross.ui.pages.objectdetails.ObjectDetailsScreen
import com.example.rmas_uross.ui.pages.profile.ProfileScreen
import com.example.rmas_uross.ui.pages.signup.SignupScreen
import com.example.rmas_uross.util.AppViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun MyAppNavigation(
    authViewModel: AuthViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val isUserLoggedIn by authViewModel.isUserLoggedIn.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentUserId = authViewModel.currentUser.collectAsState(initial = null).value?.uid ?: ""

    val startDestination = if (isUserLoggedIn) Route.home else Route.login
    val showBottomBar = currentRoute in listOf(Route.home, Route.map, Route.leaderboard, Route.profile)

    val context = LocalContext.current

    val repository: ObjectRepository = remember {
        FirebaseObjectRepository(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
    }
    val pointsRepository: PointsRepository = remember { PointsRepository() }
    val uploadRepository = remember { ImageUploadRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val locationService = remember { LocationService(context) }

    // Glavna fabrika koja kreira sve ViewModele
    val factory = remember {
        AppViewModelFactory(
            repository = repository,
            firebaseAuth = auth,
            pointsRepository = pointsRepository,
            uploadRepository = uploadRepository
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    currentRoute = currentRoute ?: Route.home,
                    onHome = {
                        navController.navigate(Route.home) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onMap = {
                        navController.navigate(Route.map) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onLeaderboard = {
                        navController.navigate(Route.leaderboard) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onProfile = {
                        navController.navigate(Route.profile) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.login) {
                LoginScreen(navController, authViewModel)
            }

            composable(Route.signup) {
                SignupScreen(navController, authViewModel)
            }

            composable(Route.home) {
                val homeViewModel: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    authViewModel = authViewModel,
                    homeViewModel = homeViewModel
                )
            }

            composable(Route.map) {
                val mapViewModel: MapViewModel = viewModel(factory = factory)
                MapScreen(
                    navController = navController,
                    viewModel = mapViewModel,
                    onBack = { navController.popBackStack() },
                    onObjectSelected = { objectId ->
                        navController.navigate(Route.objectDetailsRoute(objectId))
                    },
                    locationService = locationService,
                    currentUserId = currentUserId
                )
            }

            composable(Route.profile) {
                ProfileScreen(navController, authViewModel)
            }

            composable(Route.leaderboard) {
                val leaderboardViewModel: LeaderboardViewModel = viewModel(factory = factory)
                LeaderboardScreen(
                    viewModel = leaderboardViewModel
                )
            }

            composable(
                route = Route.objectDetails,
                arguments = listOf(navArgument("objectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val objectId = backStackEntry.arguments?.getString("objectId") ?: ""
                ObjectDetailsScreen(
                    navController = navController,
                    objectId = objectId,
                    onBack = { navController.popBackStack() },
                    repository = repository,
                    currentUserId = currentUserId
                )
            }
        }
    }
}