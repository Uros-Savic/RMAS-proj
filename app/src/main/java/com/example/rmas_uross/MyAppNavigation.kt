package com.example.rmas_uross

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.compositionLocalOf
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
import com.example.rmas_uross.ui.components.BrandTopBar
import com.example.rmas_uross.ui.pages.addobject.AddObjectScreen
import com.example.rmas_uross.ui.pages.home.HomeScreen
import com.example.rmas_uross.ui.pages.home.HomeViewModel
import com.example.rmas_uross.ui.pages.home.HomeViewModelFactory
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

val LocalHomeViewModelFactory = compositionLocalOf<HomeViewModelFactory> {
    error("HomeViewModelFactory nije pronađen")
}

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
    val showTopBar = currentRoute !in listOf(Route.login, Route.signup)
    val showBackIcon = currentRoute !in listOf(Route.home, Route.map, Route.leaderboard, Route.profile, Route.login, Route.signup)
    val context = LocalContext.current
    val repository: ObjectRepository = remember {
        FirebaseObjectRepository(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
    }
    val pointsRepository: PointsRepository = remember { PointsRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val factory = remember {
        AppViewModelFactory(
            repository = repository,
            firebaseAuth = auth,
            pointsRepository = pointsRepository
        )
    }

    val uploadRepository = remember { ImageUploadRepository() }
    val homeViewModelFactory = remember { HomeViewModelFactory(uploadRepository) }
    val locationService = remember { LocationService(context) }

    CompositionLocalProvider(
        LocalHomeViewModelFactory provides homeViewModelFactory
    ) {
        Scaffold(
            topBar = {
                if (showTopBar) {
                    BrandTopBar(
                        appName = "Klupice i česme",
                        showBack = showBackIcon,
                        onBack = { navController.popBackStack() }
                    )
                }
            },
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
                    val homeViewModel: HomeViewModel = viewModel(factory = LocalHomeViewModelFactory.current)
                    HomeScreen(navController, authViewModel, homeViewModel)
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
                        locationService = locationService
                    )
                }

                composable(Route.profile) {
                    ProfileScreen(navController, authViewModel)
                }

                composable(Route.leaderboard) {
                    val leaderboardViewModel: LeaderboardViewModel = viewModel(factory = factory)
                    LeaderboardScreen(
                        viewModel = leaderboardViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Route.createObject) {
                    AddObjectScreen(
                        navController = navController,
                        initialLatitude = null,
                        initialLongitude = null,
                        initialObjectType = null,
                        locationService = locationService,
                        viewModel = viewModel(factory = factory)
                    )
                }

                composable(
                    route = Route.addObject,
                    arguments = listOf(
                        navArgument("lat") { type = NavType.FloatType; defaultValue = -1f },
                        navArgument("lon") { type = NavType.FloatType; defaultValue = -1f }
                    )
                ) { backStackEntry ->
                    val lat = backStackEntry.arguments?.getFloat("lat")
                    val lon = backStackEntry.arguments?.getFloat("lon")

                    AddObjectScreen(
                        navController = navController,
                        initialLatitude = if (lat != -1f) lat?.toDouble() else null,
                        initialLongitude = if (lon != -1f) lon?.toDouble() else null,
                        locationService = locationService,
                        viewModel = viewModel(factory = factory)
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
}