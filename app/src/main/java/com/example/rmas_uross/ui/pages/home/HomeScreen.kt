package com.example.rmas_uross.ui.pages.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.rmas_uross.ui.auth.AuthViewModel

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel
) {
    val userName by homeViewModel.userName.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (userName.isNotEmpty()) {
            Text(
                text = userName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            CircularProgressIndicator()
        }
    }
}