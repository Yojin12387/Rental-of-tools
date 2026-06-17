package com.example.diplomki

data class Order(
    val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val days: Int = 0,
    val totalPrice: Double = 0.0,
    val status: String = "pending",
    val createdAt: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val userPhone: String = "",
    val userAddress: String = ""
) {
    val statusText: String
        get() = when (status) {
            "pending" -> "Ожидание"
            "confirmed" -> "Подтвержден"
            "in_progress" -> "В процессе"
            "completed" -> "Завершен"
            "cancelled" -> "Отменен"
            else -> status
        }

    val statusColor: androidx.compose.ui.graphics.Color
        get() = when (status) {
            "pending" -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
            "confirmed" -> androidx.compose.ui.graphics.Color(0xFF3B82F6)
            "in_progress" -> androidx.compose.ui.graphics.Color(0xFF8B5CF6)
            "completed" -> androidx.compose.ui.graphics.Color(0xFF10B981)
            "cancelled" -> androidx.compose.ui.graphics.Color(0xFFEF4444)
            else -> androidx.compose.ui.graphics.Color.Gray
        }
}