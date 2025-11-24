package com.example.rmas_uross.data.model

import java.util.*

data class ImageUpload(
    val id: String = UUID.randomUUID().toString(),
    val imageUrl: String = "",
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val location: String = "",
    val userId: String = "",
    val userName: String = "",
    val imageBase64: String = ""
) {
    companion object {
        fun createEmpty(): ImageUpload {
            return ImageUpload()
        }
    }
}