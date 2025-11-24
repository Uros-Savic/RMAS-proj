package com.example.rmas_uross.ui.pages.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rmas_uross.data.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _notifications.value = listOf(
                    Notification(
                        id = "1",
                        title = "Dobrodošli!",
                        message = "Uspešno ste se prijavili u aplikaciju",
                        type = "INFO",
                        read = false
                    ),
                    Notification(
                        id = "2",
                        title = "Nova funkcionalnost",
                        message = "Dodata je mogućnost ocenjivanja objekata",
                        type = "UPDATE",
                        read = true
                    )
                )
            } catch (e: Exception) {
                _notifications.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            _notifications.value = _notifications.value.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(read = true)
                } else {
                    notification
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            _notifications.value = _notifications.value.map { it.copy(read = true) }
        }
    }

    fun refreshNotifications() {
        loadNotifications()
    }
}