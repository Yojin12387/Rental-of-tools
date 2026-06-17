package com.example.diplomki

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppSettings {
    // Состояние темы
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Состояние языка
    private val _currentLanguage = MutableStateFlow("ru")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Текущий язык для UI
    var language by mutableStateOf("Русский")
        private set

    // Переключение темы
    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // Установка темы
    fun setTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    // Установка языка
    fun setLanguage(lang: String, langCode: String) {
        language = lang
        _currentLanguage.value = langCode
    }
}

// Цвета для светлой темы
object LightThemeColors {
    val background = Color(0xFFF5F5F5)
    val surface = Color.White
    val primary = Color(0xFF1E3A8A)
    val secondary = Color(0xFFFF7B00)
    val text = Color(0xFF1E293B)
    val textSecondary = Color.Gray
}

// Цвета для темной темы
object DarkThemeColors {
    val background = Color(0xFF121212)
    val surface = Color(0xFF1E1E1E)
    val primary = Color(0xFF3B5B9A)
    val secondary = Color(0xFFFF8C33)
    val text = Color.White
    val textSecondary = Color.LightGray
}