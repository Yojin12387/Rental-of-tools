package com.example.diplomki

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun registerUser(email: String, password: String, name: String, phone: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()

                val user = hashMapOf(
                    "userId" to authResult.user?.uid,
                    "email" to email,
                    "name" to name,
                    "phone" to phone,
                    "createdAt" to Timestamp.now()
                )

                db.collection("users")
                    .document(authResult.user?.uid ?: "")
                    .set(user)
                    .await()

                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Ошибка регистрации")
            }
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Ошибка входа")
            }
        }
    }

    fun logout() {
        auth.signOut()
    }
}

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}