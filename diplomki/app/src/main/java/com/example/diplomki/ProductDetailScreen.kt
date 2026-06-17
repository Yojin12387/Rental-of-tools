package com.example.diplomki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplomki.services.EmailService
import com.example.diplomki.screens.ContactDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    navController: NavController,
    onRentClick: (Product) -> Unit
) {
    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var isFavorite by remember { mutableStateOf(false) }
    var selectedRentDays by remember { mutableStateOf(1) }

    // Новые состояния для заказа
    var isOrderProcessing by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<UserProfile?>(null) }
    var orderedProduct by remember { mutableStateOf<Product?>(null) }
    var orderedDays by remember { mutableStateOf(1) }
    var orderedTotalPrice by remember { mutableStateOf(0.0) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Загрузка данных о продукте из Firebase
    LaunchedEffect(productId) {
        try {
            val loadedProduct = FirebaseManager.getProductById(productId)
            product = loadedProduct
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    // Загрузка текущего пользователя
    LaunchedEffect(Unit) {
        FirebaseManager.getCurrentUserProfile(
            onSuccess = { user -> currentUser = user },
            onError = { error -> println("Ошибка загрузки пользователя: $error") }
        )
    }

    // Загрузка статуса избранного
    LaunchedEffect(productId) {
        if (FirebaseManager.isUserLoggedIn) {
            isFavorite = FirebaseManager.isFavorite(productId)
        }
    }

    // Функция для toggle избранного
    fun toggleFavorite() {
        scope.launch {
            val success = FirebaseManager.toggleFavorite(productId)
            if (success) {
                isFavorite = !isFavorite
                snackbarHostState.showSnackbar(
                    message = if (isFavorite) "✅ Добавлено в избранное" else "❌ Удалено из избранного",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        // Показываем индикатор загрузки
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF7B00))
            }
            return@Scaffold
        }

        // Показываем ошибку
        if (error != null || product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Ошибка загрузки",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error ?: "Продукт не найден",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF7B00)
                        )
                    ) {
                        Text("Вернуться назад")
                    }
                }
            }
            return@Scaffold
        }

        val currentProduct = product!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            // TopAppBar
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "В избранное",
                            tint = if (isFavorite) Color.Red else Color.Gray
                        )
                    }
                    IconButton(onClick = { /* Поделиться */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поделиться"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    actionIconContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )

            // ОСНОВНОЙ КОНТЕНТ
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // Изображение товара
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentProduct.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = currentProduct.imageUrl,
                            contentDescription = currentProduct.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFE5E7EB),
                                            Color(0xFFD1D5DB)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🛠️",
                                fontSize = 80.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(
                                color = if (currentProduct.available) Color(0xFF10B981) else Color(0xFFEF4444),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentProduct.available) "Доступно" else "Занято",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = currentProduct.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Рейтинг и категория
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFFF7B00).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "★ ${String.format("%.1f", currentProduct.rating)}",
                                    color = Color(0xFFFF7B00),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentProduct.category,
                                color = Color(0xFF6B7280),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Описание",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentProduct.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF4B5563),
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Характеристики
                    Text(
                        text = "Характеристики",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentProduct.characteristics.isNotEmpty()) {
                        currentProduct.characteristics.forEach { (key, value) ->
                            DetailCharacteristicsItem(title = key, value = value)
                        }
                    } else {
                        DetailCharacteristicsItem(title = "Категория", value = currentProduct.category)
                        DetailCharacteristicsItem(title = "Состояние", value = "Отличное")
                        DetailCharacteristicsItem(title = "Минимальный срок аренды", value = "1 день")
                        DetailCharacteristicsItem(title = "Залог", value = "Не требуется")
                        DetailCharacteristicsItem(title = "Доставка", value = "Доступна")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Отзывы
                    Text(
                        text = "Отзывы",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailReviewItem(
                        author = "Алексей Петров",
                        rating = 5.0,
                        comment = "Отличный инструмент, работал без нареканий.",
                        date = "15.12.2024"
                    )

                    DetailReviewItem(
                        author = "Мария Иванова",
                        rating = 4.0,
                        comment = "Хороший инструмент, немного шумный.",
                        date = "10.12.2024"
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // БЛОК С КНОПКАМИ
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 16.dp
                    )
                ) {
                    Text(
                        text = "Срок аренды",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf(1, 3, 7, 14).forEach { days ->
                                DetailDayChip(
                                    days = days,
                                    isSelected = selectedRentDays == days,
                                    onClick = { selectedRentDays = days }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }

                        Text(
                            text = "$selectedRentDays ${getDetailDayText(selectedRentDays)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Итого",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = "${(currentProduct.price * selectedRentDays).toInt()} ₽",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF7B00)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ДВЕ КНОПКИ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    if (FirebaseManager.isUserLoggedIn) {
                                        addToCart(
                                            product = currentProduct,
                                            days = selectedRentDays,
                                            snackbarHostState = snackbarHostState,
                                            navController = navController
                                        )
                                    } else {
                                        navController.navigate("login")
                                    }
                                }
                            },
                            enabled = currentProduct.available && !isOrderProcessing,
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF3F4F6),
                                contentColor = Color(0xFF1E3A8A)
                            )
                        ) {
                            Text(
                                text = if (currentProduct.available) "В корзину" else "Недоступно",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (!isOrderProcessing && currentProduct.available) {
                                    if (currentUser == null) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "❌ Необходимо войти в аккаунт",
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                        navController.navigate("login")
                                    } else {
                                        scope.launch {
                                            isOrderProcessing = true

                                            try {
                                                // 1. Обновляем статус товара на "занято"
                                                val updateSuccess = FirebaseManager.markProductAsBusy(currentProduct.id)

                                                if (updateSuccess) {
                                                    // 2. Создаем заказ с изображением
                                                    val startDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                                                    val endDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(
                                                        Date(System.currentTimeMillis() + selectedRentDays * 24 * 60 * 60 * 1000)
                                                    )
                                                    val currentTime = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

                                                    val orderId = FirebaseManager.createOrder(
                                                        productId = currentProduct.id,
                                                        productName = currentProduct.name,
                                                        productImage = currentProduct.imageUrl,
                                                        days = selectedRentDays,
                                                        totalPrice = currentProduct.price * selectedRentDays,
                                                        startDate = startDate,
                                                        endDate = endDate,
                                                        userPhone = currentUser!!.phone,
                                                        userAddress = currentUser!!.address
                                                    )

                                                    if (orderId != null) {
                                                        println("✅ Заказ создан с ID: $orderId")

                                                        // 3. Отправляем email уведомление
                                                        try {
                                                            val emailResult = EmailService.sendOrderNotification(
                                                                user = currentUser!!,
                                                                product = currentProduct,
                                                                days = selectedRentDays,
                                                                startDate = startDate,
                                                                endDate = endDate,
                                                                currentTime = currentTime
                                                            )

                                                            emailResult.onSuccess {
                                                                println("✅ Email отправлен")
                                                            }.onFailure { e ->
                                                                println("❌ Ошибка отправки email: ${e.message}")
                                                            }
                                                        } catch (e: Exception) {
                                                            println("❌ Ошибка при отправке email: ${e.message}")
                                                        }

                                                        // 4. Обновляем локальный объект товара
                                                        val updatedProduct = currentProduct.copy(available = false)
                                                        product = updatedProduct

                                                        // 5. Сохраняем данные для диалога
                                                        orderedProduct = updatedProduct
                                                        orderedDays = selectedRentDays
                                                        orderedTotalPrice = currentProduct.price * selectedRentDays

                                                        // 6. Показываем диалог с контактами
                                                        showContactDialog = true

                                                        // 7. Показываем сообщение об успехе
                                                        snackbarHostState.showSnackbar(
                                                            message = "✅ Заказ успешно оформлен!",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    } else {
                                                        snackbarHostState.showSnackbar(
                                                            message = "❌ Ошибка при создании заказа",
                                                            duration = SnackbarDuration.Long
                                                        )
                                                    }
                                                } else {
                                                    snackbarHostState.showSnackbar(
                                                        message = "❌ Не удалось забронировать товар. Возможно, он уже занят.",
                                                        duration = SnackbarDuration.Long
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar(
                                                    message = "❌ Ошибка: ${e.message}",
                                                    duration = SnackbarDuration.Long
                                                )
                                            } finally {
                                                isOrderProcessing = false
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = currentProduct.available && !isOrderProcessing,
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF7B00),
                                contentColor = Color.White,
                                disabledContainerColor = Color.Gray
                            )
                        ) {
                            if (isOrderProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Арендовать",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог с контактами
    if (showContactDialog && orderedProduct != null) {
        ContactDialog(
            onDismiss = {
                showContactDialog = false
                navController.popBackStack()
            },
            productName = orderedProduct!!.name,
            totalPrice = orderedTotalPrice,
            days = orderedDays
        )
    }
}

@Composable
fun DetailCharacteristicsItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E3A8A)
        )
    }
}

private suspend fun addToCart(
    product: Product,
    days: Int,
    snackbarHostState: SnackbarHostState,
    navController: NavController
) {
    try {
        val success = FirebaseManager.addToCart(product.id, days)
        if (success) {
            snackbarHostState.showSnackbar(
                message = "✅ Товар добавлен в корзину",
                duration = SnackbarDuration.Short
            )
        } else {
            snackbarHostState.showSnackbar(
                message = "❌ Не удалось добавить в корзину",
                duration = SnackbarDuration.Long
            )
        }
    } catch (e: Exception) {
        snackbarHostState.showSnackbar(
            message = "❌ Ошибка: ${e.message}",
            duration = SnackbarDuration.Long
        )
    }
}

@Composable
fun DetailReviewItem(author: String, rating: Double, comment: String, date: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFF7B00).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "★ $rating",
                        color = Color(0xFFFF7B00),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = comment,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4B5563),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun DetailDayChip(days: Int, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text("$days д") },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFF7B00),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFFF3F4F6),
            labelColor = Color(0xFF6B7280)
        )
    )
}

fun getDetailDayText(days: Int): String {
    return when {
        days % 10 == 1 && days % 100 != 11 -> "день"
        days % 10 in 2..4 && days % 100 !in 12..14 -> "дня"
        else -> "дней"
    }
}