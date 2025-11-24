package com.example.rmas_uross.ui.interactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rmas_uross.data.model.Interaction
import com.example.rmas_uross.data.repository.InteractionRepository
import com.example.rmas_uross.data.repository.PointsRepository
import com.example.rmas_uross.util.PointsSystem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InteractionViewModel(
    private val interactionRepository: InteractionRepository,
    private val pointsRepository: PointsRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _interactionResult = MutableStateFlow<InteractionResult?>(null)
    val interactionResult: StateFlow<InteractionResult?> = _interactionResult.asStateFlow()

    fun rateObject(
        objectId: String,
        objectName: String,
        rating: Int,
        state: String,
        comment: String = ""
    ) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val alreadyRated = interactionRepository.hasUserInteracted(
                    userId,
                    objectId,
                    Interaction.TYPE_RATING
                )

                if (alreadyRated) {
                    _interactionResult.value =
                        InteractionResult.Error("Već ste ocenili ovaj objekat")
                    return@launch
                }

                val interaction = Interaction(
                    objectId = objectId,
                    userId = userId,
                    type = Interaction.TYPE_RATING,
                    rating = rating,
                    state = state,
                    comment = comment,
                    pointsAwarded = PointsSystem.POINTS_ADD_RATING
                )

                interactionRepository.addInteraction(interaction)

                pointsRepository.awardPoints(
                    userId = userId,
                    actionType = "ADD_RATING",
                    targetId = objectId,
                    metadata = mapOf(
                        "rating" to rating,
                        "state" to state,
                        "commentLength" to comment.length
                    )
                )

                _interactionResult.value = InteractionResult.Success(
                    message = "Hvala na oceni! +${PointsSystem.POINTS_ADD_RATING} poena",
                    pointsAwarded = PointsSystem.POINTS_ADD_RATING
                )

            } catch (e: Exception) {
                _interactionResult.value = InteractionResult.Error("Greška: ${e.message}")
            }
        }
    }

    fun reportProblem(
        objectId: String,
        objectName: String,
        problemType: String,
        description: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val interaction = Interaction(
                    objectId = objectId,
                    userId = userId,
                    type = Interaction.TYPE_REPORT,
                    problemType = problemType,
                    comment = description,
                    pointsAwarded = PointsSystem.POINTS_CONFIRM_STATE
                )

                interactionRepository.addInteraction(interaction)

                pointsRepository.awardPoints(
                    userId = userId,
                    actionType = "CONFIRM_STATE",
                    targetId = objectId,
                    metadata = mapOf(
                        "problemType" to problemType,
                        "descriptionLength" to description.length
                    )
                )

                _interactionResult.value = InteractionResult.Success(
                    message = "Problem je prijavljen! +${PointsSystem.POINTS_CONFIRM_STATE} poena",
                    pointsAwarded = PointsSystem.POINTS_CONFIRM_STATE
                )

            } catch (e: Exception) {
                _interactionResult.value = InteractionResult.Error("Greška: ${e.message}")
            }
        }

        fun likeObject(objectId: String) {
            val userId = auth.currentUser?.uid ?: return

            viewModelScope.launch {
                try {
                    val alreadyLiked = interactionRepository.hasUserInteracted(
                        userId,
                        objectId,
                        Interaction.TYPE_LIKE
                    )

                    val interaction = Interaction(
                        objectId = objectId,
                        userId = userId,
                        type = Interaction.TYPE_LIKE,
                        pointsAwarded = if (!alreadyLiked) 2 else 0
                    )

                    interactionRepository.addInteraction(interaction)

                    if (!alreadyLiked) {
                        pointsRepository.awardPoints(
                            userId = userId,
                            actionType = "LIKE_OBJECT",
                            targetId = objectId
                        )
                    }

                    _interactionResult.value = InteractionResult.Success(
                        message = if (!alreadyLiked) "Sviđa vam se! +2 poena" else "Već vam se sviđa",
                        pointsAwarded = if (!alreadyLiked) 2 else 0
                    )

                } catch (e: Exception) {
                    _interactionResult.value = InteractionResult.Error("Greška: ${e.message}")
                }
            }
        }

        fun clearResult() {
            _interactionResult.value = null
        }
    }

    sealed class InteractionResult {
        data class Success(val message: String, val pointsAwarded: Int) : InteractionResult()
        data class Error(val message: String) : InteractionResult()
    }
}