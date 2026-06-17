package com.example.diplomki

data class Review(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val productId: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    val createdAt: String = ""
)