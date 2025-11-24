package com.example.rmas_uross.ui.pages.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rmas_uross.data.repository.PointsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val pointsRepository: PointsRepository
) : ViewModel() {

    private val _leaderboard = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardUser>> = _leaderboard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadLeaderboard() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val users = pointsRepository.getLeaderboard()
                val leaderboardData = users.map { user ->
                    LeaderboardUser(
                        userId = user.uid.ifEmpty { user.uid },
                        username = user.username.ifEmpty { user.fullName.ifEmpty { "Anoniman korisnik" } },
                        points = user.points.toInt(),
                        level = user.level,
                        rank = user.rank,
                        experience = user.experience.toInt(),
                        fullName = user.fullName,
                        objectsAdded = user.objectsAdded,
                        reviewsWritten = user.reviewsWritten,
                        interactions = user.interactions
                    )
                }

                _leaderboard.value = leaderboardData
            } catch (e: Exception) {
                _error.value = "Greška pri učitavanju rang liste: ${e.message}"
                _leaderboard.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun refreshLeaderboard() {
        loadLeaderboard()
    }
}

data class LeaderboardUser(
    val userId: String = "",
    val username: String,
    val points: Int,
    val level: Int,
    val rank: String,
    val experience: Int,
    val profileImage: String = "",
    val fullName: String = "",
    val objectsAdded: Int = 0,
    val reviewsWritten: Int = 0,
    val interactions: Int = 0
) {
    fun getProgressToNextLevel(): Float {
        val experienceForNextLevel = level * 1000
        return experience.toFloat() / experienceForNextLevel
    }

    fun getRemainingExperience(): Int {
        val experienceForNextLevel = level * 1000
        return maxOf(0, experienceForNextLevel - experience)
    }
}