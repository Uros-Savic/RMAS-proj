package com.example.rmas_uross.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.rmas_uross.data.repository.ImageUploadRepository
import com.example.rmas_uross.data.repository.ObjectRepository
import com.example.rmas_uross.data.repository.PointsRepository
import com.example.rmas_uross.ui.pages.home.HomeViewModel
import com.example.rmas_uross.ui.pages.leaderboard.LeaderboardViewModel
import com.example.rmas_uross.ui.pages.map.MapViewModel
import com.google.firebase.auth.FirebaseAuth

class AppViewModelFactory(
    private val repository: ObjectRepository,
    private val firebaseAuth: FirebaseAuth,
    private val pointsRepository: PointsRepository,
    private val uploadRepository: ImageUploadRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(firebaseAuth) as T
            }
            modelClass.isAssignableFrom(MapViewModel::class.java) -> {
                MapViewModel(repository, firebaseAuth, pointsRepository) as T
            }
            modelClass.isAssignableFrom(LeaderboardViewModel::class.java) -> {
                LeaderboardViewModel(pointsRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}