package com.example.diplomki.models

data class User(
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val phone: String = "",
    val address: String = "",
    val registrationDate: String = "",
    val totalOrders: Int = 0,
    val totalSpent: Double = 0.0,
    val rating: Double = 0.0,
    val avatarUrl: String? = null
) {
    companion object {
        fun createEmpty(): User {
            return User(
                id = "",
                email = "",
                fullName = "Пользователь",
                phone = "Не указан",
                address = "Не указан",
                registrationDate = "",
                totalOrders = 0,
                totalSpent = 0.0,
                rating = 0.0,
                avatarUrl = null
            )
        }
    }
}