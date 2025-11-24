package com.example.rmas_uross.ui.pages.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.rmas_uross.data.repository.ImageUploadRepository

class HomeViewModelFactory(
    private val uploadRepository: ImageUploadRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(uploadRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}