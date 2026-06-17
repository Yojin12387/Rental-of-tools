package com.example.diplomki.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.diplomki.FirebaseManager
import com.example.diplomki.Product
import kotlinx.coroutines.launch
import com.example.diplomki.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavHostController
) {
    var favoriteProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Функция загрузки избранного
    fun loadFavorites() {
        scope.launch {
            isLoading = true
            try {
                favoriteProducts = FirebaseManager.getFavoriteProducts()
                println("📦 Загружено избранных товаров: ${favoriteProducts.size}")
            } catch (e: Exception) {
                println("❌ Ошибка загрузки избранного: ${e.message}")
                snackbarHostState.showSnackbar(
                    message = "Ошибка загрузки избранного",
                    duration = SnackbarDuration.Long
                )
            }
            isLoading = false
        }
    }

    // Загрузка избранных товаров при открытии экрана
    LaunchedEffect(Unit) {
        loadFavorites()
    }

    // Функция удаления из избранного
    fun removeFromFavorites(productId: String) {
        scope.launch {
            val success = FirebaseManager.toggleFavorite(productId)
            if (success) {
                // Удаляем из локального списка
                favoriteProducts = favoriteProducts.filter { it.id != productId }
                snackbarHostState.showSnackbar(
                    message = "✅ Товар удален из избранного",
                    duration = SnackbarDuration.Short
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = "❌ Ошибка при удалении",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    // Функция очистки всего избранного
    fun clearAllFavorites() {
        scope.launch {
            var allSuccess = true
            for (product in favoriteProducts) {
                val success = FirebaseManager.toggleFavorite(product.id)
                if (!success) allSuccess = false
            }
            if (allSuccess) {
                favoriteProducts = emptyList()
                snackbarHostState.showSnackbar(
                    message = "✅ Избранное очищено",
                    duration = SnackbarDuration.Short
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = "❌ Ошибка при очистке",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Избранное",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    if (favoriteProducts.isNotEmpty()) {
                        IconButton(
                            onClick = { clearAllFavorites() }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Очистить всё",
                                tint = Color.Red
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E3A8A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            if (isLoading) {
                // Состояние загрузки
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF7B00))
                }
            } else if (favoriteProducts.isEmpty()) {
                // Пустое состояние
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FavoriteBorder,
                            contentDescription = "Нет избранных",
                            modifier = Modifier.size(80.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = "В избранном пока пусто",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "Добавляйте товары в избранное,\nчтобы они появились здесь",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                navController.navigate("products") {
                                    popUpTo("products") { inclusive = true }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF7B00)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Перейти к товарам")
                        }
                    }
                }
            } else {
                // Список избранных товаров
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = favoriteProducts,
                        key = { it.id }
                    ) { product ->
                        FavoriteProductCard(
                            product = product,
                            onProductClick = {
                                navController.navigate("product/${product.id}")
                            },
                            onRemoveClick = {
                                removeFromFavorites(product.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteProductCard(
    product: Product,
    onProductClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Изображение
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF3F4F6))
            ) {
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛠️", fontSize = 32.sp)
                    }
                }

                // Индикатор доступности
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (product.available) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Информация
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = product.category,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Рейтинг
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFFF7B00).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "★ ${String.format("%.1f", product.rating)}",
                            fontSize = 10.sp,
                            color = Color(0xFFFF7B00),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Цена
                    Text(
                        text = "${product.price.toInt()} ₽/день",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF7B00)
                    )
                }
            }

            // Кнопка удаления
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Удалить",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}