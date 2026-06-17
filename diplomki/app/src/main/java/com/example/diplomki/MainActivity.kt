package com.example.diplomki

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.diplomki.screens.*
import com.example.diplomki.ui.theme.DiplomkiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            TestData.initializeProductsIfNeeded()
        }

        setContent {
            DiplomkiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ToolRentalApp()
                }
            }
        }
    }
}

@Composable
fun ToolRentalApp() {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseManager.checkAuthState { isLoggedIn ->
            startDestination = if (isLoggedIn) {
                "products"
            } else {
                "login"
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFF7B00))
        }
        return
    }

    if (startDestination == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFF7B00))
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = startDestination!!
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("products") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onBack = {
                    (navController.context as? Activity)?.finish()
                },
                onRegisterClick = {
                    navController.navigate("registration")
                }
            )
        }

        composable("registration") {
            RegistrationScreen(
                onRegisterSuccess = {
                    navController.navigate("products") {
                        popUpTo("registration") { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.navigate("login") {
                        popUpTo("registration") { inclusive = true }
                    }
                }
            )
        }

        composable("products") {
            if (!FirebaseManager.isUserLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                return@composable
            }

            ProductsScreen(
                navController = navController,
                onProductClick = { product ->
                    navController.navigate("product/${product.id}")
                },
                onOrdersClick = {
                    navController.navigate("cart")
                },
                onSettingsClick = {
                    // Убрали настройки
                },
                onLogout = {
                    FirebaseManager.logout()
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }

        composable("cart") {
            if (!FirebaseManager.isUserLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                return@composable
            }

            CartScreen(navController = navController)
        }

        composable("profile") {
            if (!FirebaseManager.isUserLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                return@composable
            }

            ProfileScreen(
                navController = navController,
                onLogout = {
                    FirebaseManager.logout()
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }

        composable(
            "product/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                navController = navController,
                onRentClick = { product ->
                    println("Аренда товара: ${product.name}")
                }
            )
        }

        composable(
            "editProfile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            if (!FirebaseManager.isUserLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                return@composable
            }

            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            var userProfile by remember { mutableStateOf<com.example.diplomki.UserProfile?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(userId) {
                FirebaseManager.getCurrentUserProfile(
                    onSuccess = { profile ->
                        userProfile = profile
                        isLoading = false
                    },
                    onError = {
                        isLoading = false
                        navController.popBackStack()
                    }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF7B00))
                }
            } else if (userProfile != null) {
                EditProfileScreen(
                    navController = navController,
                    currentUserProfile = com.example.diplomki.models.UserProfile(
                        id = userProfile!!.id,
                        email = userProfile!!.email,
                        name = userProfile!!.name,
                        phone = userProfile!!.phone,
                        address = userProfile!!.address,
                        createdAt = userProfile!!.createdAt,
                        avatarUrl = null,
                        totalOrders = userProfile!!.totalOrders,
                        totalSpent = userProfile!!.totalSpent,
                        rating = userProfile!!.rating
                    )
                )
            }
        }

        composable("admin") {
            if (!FirebaseManager.isUserLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                return@composable
            }

            AdminScreen(navController = navController)
        }

        composable("favorites") {
            if (!FirebaseManager.isUserLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                return@composable
            }

            FavoritesScreen(navController = navController)
        }

        composable("orders") {
            if (!FirebaseManager.isUserLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                return@composable
            }

            OrdersHistoryScreen(navController = navController)
        }

        // Убрали composable("settings")
    }
}