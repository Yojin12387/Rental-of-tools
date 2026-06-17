package com.example.diplomki

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "",
    val rating: Double = 0.0,
    val available: Boolean = true,
    val characteristics: Map<String, String> = emptyMap()
)