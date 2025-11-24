package com.example.rmas_uross.utils

import androidx.compose.runtime.Composable

// Jednostavna placeholder funkcija - vraća true dok ne implementirate prave permisije
@Composable
fun rememberImagePermissionState(): Boolean {
    // Za sada vraćamo true da aplikacija može da radi
    // Kasnije možete implementirati pravu permission logiku
    return true
}