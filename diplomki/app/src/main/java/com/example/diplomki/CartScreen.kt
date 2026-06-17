package com.example.diplomki.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.diplomki.FirebaseManager
import com.example.diplomki.Product
import com.example.diplomki.UserProfile
import com.example.diplomki.bd.CartProduct
import com.example.diplomki.services.EmailService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavHostController
) {
    var cartProducts by remember { mutableStateOf<List<CartProduct>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalPrice by remember { mutableStateOf(0.0) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Состояния для заказа
    var isOrderProcessing by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<UserProfile?>(null) }
    var orderedProducts by remember { mutableStateOf<List<CartProduct>>(emptyList()) }
    var orderedTotalPrice by remember { mutableStateOf(0.0) }

    // Загрузка текущего пользователя
    LaunchedEffect(Unit) {
        FirebaseManager.getCurrentUserProfile(
            onSuccess = { user -> currentUser = user },
            onError = { error -> println("Ошибка загрузки пользователя: $error") }
        )
    }

    // Функция загрузки корзины
    fun loadCart() {
        scope.launch {
            try {
                isLoading = true
                val cartItems = FirebaseManager.getCartProducts()
                cartProducts = cartItems
                // Расчет общей стоимости
                totalPrice = cartItems.sumOf { it.product.price * it.days * it.quantity }
                isLoading = false
            } catch (e: Exception) {
                println("Ошибка загрузки корзины: ${e.message}")
                isLoading = false
            }
        }
    }

    // Загрузка при первом открытии
    LaunchedEffect(Unit) {
        loadCart()
    }

    // Функция обновления количества дней
    fun updateDays(item: CartProduct, newDays: Int) {
        if (newDays > 0) {
            scope.launch {
                val success = FirebaseManager.updateCartItemDays(item.cartItemId, newDays)
                if (success) {
                    // Обновляем локальное состояние
                    cartProducts = cartProducts.map {
                        if (it.cartItemId == item.cartItemId) {
                            it.copy(days = newDays)
                        } else {
                            it
                        }
                    }
                    // Пересчитываем стоимость
                    totalPrice = cartProducts.sumOf { it.product.price * it.days * it.quantity }
                } else {
                    snackbarHostState.showSnackbar(
                        message = "Не удалось обновить срок аренды",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Функция удаления товара
    fun removeItem(item: CartProduct) {
        scope.launch {
            val success = FirebaseManager.removeCartItem(item.cartItemId)
            if (success) {
                // Удаляем из локального состояния
                cartProducts = cartProducts.filter { it.cartItemId != item.cartItemId }
                // Пересчитываем стоимость
                totalPrice = cartProducts.sumOf { it.product.price * it.days * it.quantity }
                snackbarHostState.showSnackbar(
                    message = "Товар удален из корзины",
                    duration = SnackbarDuration.Short
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = "Не удалось удалить товар",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    // Функция оформления заказа из корзины
    fun checkout() {
        if (isOrderProcessing) return

        if (currentUser == null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "❌ Необходимо войти в аккаунт",
                    duration = SnackbarDuration.Long
                )
            }
            navController.navigate("login")
            return
        }

        if (cartProducts.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "❌ Корзина пуста",
                    duration = SnackbarDuration.Long
                )
            }
            return
        }

        scope.launch {
            isOrderProcessing = true

            try {
                var allSuccess = true
                val failedProducts = mutableListOf<String>()
                val successfulProducts = mutableListOf<CartProduct>()

                // Проверяем доступность всех товаров перед заказом
                for (cartProduct in cartProducts) {
                    val product = FirebaseManager.getProductById(cartProduct.product.id)
                    if (product == null || !product.available) {
                        allSuccess = false
                        failedProducts.add(cartProduct.product.name)
                    }
                }

                if (!allSuccess) {
                    snackbarHostState.showSnackbar(
                        message = "❌ Некоторые товары уже заняты: ${failedProducts.joinToString()}",
                        duration = SnackbarDuration.Long
                    )
                    isOrderProcessing = false
                    return@launch
                }

                // Общие даты для всего заказа
                val startDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                val maxDays = cartProducts.maxOfOrNull { it.days } ?: 1
                val endDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(
                    Date(System.currentTimeMillis() + maxDays * 24 * 60 * 60 * 1000)
                )
                val currentTime = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

                // Оформляем заказы для всех товаров
                for (cartProduct in cartProducts) {
                    // Обновляем статус товара
                    val updateSuccess = FirebaseManager.markProductAsBusy(cartProduct.product.id)

                    if (updateSuccess) {
                        // Создаем заказ
                        val orderId = FirebaseManager.createOrder(
                            productId = cartProduct.product.id,
                            productName = cartProduct.product.name,
                            days = cartProduct.days,
                            totalPrice = cartProduct.product.price * cartProduct.days,
                            startDate = startDate,
                            endDate = endDate,
                            userPhone = currentUser!!.phone,
                            userAddress = currentUser!!.address
                        )

                        if (orderId != null) {
                            successfulProducts.add(cartProduct)
                        } else {
                            allSuccess = false
                            failedProducts.add(cartProduct.product.name)
                        }
                    } else {
                        allSuccess = false
                        failedProducts.add(cartProduct.product.name)
                    }
                }

                if (allSuccess && successfulProducts.isNotEmpty()) {
                    // Отправляем ОДНО письмо со всеми товарами
                    try {
                        val emailResult = EmailService.sendCartOrderNotification(
                            user = currentUser!!,
                            cartProducts = successfulProducts,
                            startDate = startDate,
                            endDate = endDate,
                            currentTime = currentTime
                        )

                        emailResult.onSuccess {
                            println("✅ Email с корзиной успешно отправлен")
                        }.onFailure { e ->
                            println("❌ Ошибка отправки email: ${e.message}")
                        }
                    } catch (e: Exception) {
                        println("❌ Ошибка при отправке email: ${e.message}")
                    }

                    // Очищаем корзину после успешного заказа
                    FirebaseManager.clearCart()

                    // Сохраняем данные для диалога
                    orderedProducts = successfulProducts
                    orderedTotalPrice = totalPrice

                    // Показываем диалог с контактами
                    showContactDialog = true

                    // Обновляем корзину
                    loadCart()
                } else {
                    snackbarHostState.showSnackbar(
                        message = "❌ Ошибка при заказе: ${failedProducts.joinToString()}",
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Корзина",
                        fontWeight = FontWeight.Bold
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
                    if (cartProducts.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val success = FirebaseManager.clearCart()
                                    if (success) {
                                        cartProducts = emptyList()
                                        totalPrice = 0.0
                                        snackbarHostState.showSnackbar(
                                            message = "Корзина очищена",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Очистить корзину",
                                tint = Color.Red
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (cartProducts.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 8.dp,
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Итого",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "${totalPrice.toInt()} ₽",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF7B00)
                                )
                            }

                            Button(
                                onClick = { checkout() },
                                modifier = Modifier
                                    .height(50.dp)
                                    .width(150.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF7B00),
                                    contentColor = Color.White
                                ),
                                enabled = totalPrice > 0 && !isOrderProcessing
                            ) {
                                if (isOrderProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Оформить",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Доставка рассчитывается при оформлении",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF7B00))
            }
        } else if (cartProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = "Пустая корзина",
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "Корзина пуста",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "Добавьте товары в корзину, чтобы увидеть их здесь",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Button(
                        onClick = {
                            navController.navigate("products") {
                                popUpTo("products") { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF7B00)
                        )
                    ) {
                        Text("Перейти к товарам")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Заголовок с количеством товаров
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Товары (${cartProducts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${cartProducts.sumOf { it.quantity }} шт.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartProducts) { item ->
                        CartItemCard(
                            item = item,
                            onDaysChange = { newDays ->
                                updateDays(item, newDays)
                            },
                            onRemove = {
                                removeItem(item)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }

    // Диалог с контактами
    if (showContactDialog && orderedProducts.isNotEmpty()) {
        ContactDialog(
            onDismiss = {
                showContactDialog = false
                navController.popBackStack() // Возврат к списку товаров
            },
            productName = "Заказ из ${orderedProducts.size} товаров",
            totalPrice = orderedTotalPrice,
            days = orderedProducts.maxOfOrNull { it.days } ?: 1
        )
    }
}

@Composable
fun CartItemCard(
    item: CartProduct,
    onDaysChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Верхняя строка: Изображение и информация
            Row {
                // Изображение товара
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.product.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = item.product.imageUrl,
                            contentDescription = item.product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text("🛠️", fontSize = 30.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Информация о товаре
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Цена за день: ${item.product.price.toInt()} ₽",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Количество (если больше 1)
                    if (item.quantity > 1) {
                        Text(
                            text = "Количество: ${item.quantity} шт.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF7B00),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Кнопка удаления
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Нижняя строка: Срок аренды и стоимость
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Выбор срока аренды
                Column {
                    Text(
                        text = "Срок аренды",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onDaysChange(item.days - 1) },
                            modifier = Modifier.size(32.dp),
                            enabled = item.days > 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Уменьшить срок",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "${item.days} ${getDayText(item.days)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = { onDaysChange(item.days + 1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Увеличить срок",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Стоимость
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Стоимость",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${(item.product.price * item.days * item.quantity).toInt()} ₽",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF7B00)
                    )
                }
            }
        }
    }
}

private fun getDayText(days: Int): String {
    return when {
        days % 10 == 1 && days % 100 != 11 -> "день"
        days % 10 in 2..4 && days % 100 !in 12..14 -> "дня"
        else -> "дней"
    }
}