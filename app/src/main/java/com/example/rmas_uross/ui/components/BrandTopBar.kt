@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.rmas_uross.ui.components


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
// Ispravan import za ikonu za nazad
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BrandTopBar(
    appName: String,
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(appName) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Nazad"
                    )
                }
            }
        },
        modifier = modifier
    )
}