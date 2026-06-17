package com.example.diplomki

data class UserStats(
    val totalOrders: Int = 0,
    val totalSpent: Double = 0.0,
    val activeOrders: Int = 0,
    val completedOrders: Int = 0
)