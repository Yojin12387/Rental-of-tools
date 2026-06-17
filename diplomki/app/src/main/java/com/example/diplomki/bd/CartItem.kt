package com.example.diplomki.models

data class CartItem(
    val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val days: Int = 1,
    val addedAt: String = "",
    val quantity: Int = 1,
    val status: String = "active" // active, reserved, ordered
)


