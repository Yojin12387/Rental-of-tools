package com.example.diplomki.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val name: String = "Пользователь",
    val phone: String = "Не указан",
    val address: String = "Не указан",
    val createdAt: String = "",
    val avatarUrl: String? = null,
    val totalOrders: Int = 0,
    val totalSpent: Double = 0.0,
    val rating: Double = 0.0,
    val favorites: List<String> = emptyList()
) {
    val formattedDate: String
        get() = try {
            val date = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH).parse(createdAt)
            if (date != null) {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)
            } else {
                "Неизвестно"
            }
        } catch (e: Exception) {
            createdAt
        }

    companion object {
        fun createEmpty(): UserProfile {
            return UserProfile(
                id = "",
                email = "",
                name = "Пользователь",
                phone = "Не указан",
                address = "Не указан",
                createdAt = "",
                avatarUrl = null,
                totalOrders = 0,
                totalSpent = 0.0,
                rating = 0.0
            )
        }
    }
}