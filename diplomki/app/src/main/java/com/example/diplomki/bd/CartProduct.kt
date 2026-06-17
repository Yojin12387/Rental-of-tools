package com.example.diplomki.bd

import com.example.diplomki.Product

data class CartProduct(
    val cartItemId: String = "",
    val product: Product = Product(), // Добавил значение по умолчанию
    val days: Int = 1,
    val quantity: Int = 1, // ДОБАВИЛ ЭТО ПОЛЕ!
    val totalPrice: Double = 0.0
)