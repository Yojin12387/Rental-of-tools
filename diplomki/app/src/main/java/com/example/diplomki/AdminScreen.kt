package com.example.diplomki.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.diplomki.FirebaseManager
import com.example.diplomki.Order
import com.example.diplomki.Product
import com.example.diplomki.UserProfile
import kotlinx.coroutines.launch
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material.icons.filled.ArrowDropUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    navController: NavHostController
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isAdmin by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Проверка прав админа
    LaunchedEffect(Unit) {
        isAdmin = FirebaseManager.isCurrentUserAdmin()
        if (!isAdmin) {
            navController.popBackStack()
        }
        isLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Панель администратора",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E3A8A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF5F5F5))
            ) {
                // Табы
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF1E3A8A),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFFF7B00)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Товары",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Заказы",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "Пользователи",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            Text(
                                "Статистика",
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }

                // Контент в зависимости от таба
                when (selectedTab) {
                    0 -> ProductsTab(navController, snackbarHostState)
                    1 -> OrdersTab(snackbarHostState)
                    2 -> UsersTab(snackbarHostState)
                    3 -> StatsTab()
                }
            }
        }
    }
}

@Composable
fun ProductsTab(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState
) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Product?>(null) } // Для подтверждения удаления
    val scope = rememberCoroutineScope()

    // Используем Flow для автоматического обновления
    LaunchedEffect(Unit) {
        FirebaseManager.getProductsStream().collectLatest { productList ->
            products = productList
            isLoading = false
        }
    }

    fun updateProductLocally(updatedProduct: Product) {
        products = products.map {
            if (it.id == updatedProduct.id) updatedProduct else it
        }
    }

    fun removeProductLocally(productId: String) {
        products = products.filter { it.id != productId }
    }

    fun addProductLocally(newProduct: Product) {
        products = products + newProduct
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Подтверждение удаления") },
            text = { Text("Вы уверены, что хотите удалить товар \"${showDeleteDialog?.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val productToDelete = showDeleteDialog
                        showDeleteDialog = null
                        if (productToDelete != null) {
                            scope.launch {
                                // Сначала обновляем UI
                                removeProductLocally(productToDelete.id)

                                val result = FirebaseManager.deleteProduct(productToDelete.id)
                                result.onFailure { e ->
                                    // Если ошибка, возвращаем товар обратно
                                    addProductLocally(productToDelete)
                                    snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                                }
                            }
                        }
                    }
                ) {
                    Text("Удалить", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading && products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF7B00))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = products,
                    key = { it.id }
                ) { product ->
                    AdminProductCard(
                        product = product,
                        onEdit = { selectedProduct = product },
                        onDelete = { showDeleteDialog = product }, // Открываем диалог подтверждения
                        onToggleAvailability = {
                            scope.launch {
                                // Сначала обновляем UI (плавно)
                                val updatedProduct = product.copy(available = !product.available)
                                updateProductLocally(updatedProduct)

                                val result = FirebaseManager.updateProductAvailability(
                                    product.id,
                                    !product.available
                                )
                                result.onFailure { e ->
                                    // Если ошибка, возвращаем обратно
                                    updateProductLocally(product)
                                    snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                                }
                            }
                        }
                    )
                }
            }
        }

        // Кнопка добавления
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color(0xFFFF7B00),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
        }
    }

    // Диалог добавления/редактирования
    if (showAddDialog || selectedProduct != null) {
        ProductDialog(
            product = selectedProduct,
            onDismiss = {
                showAddDialog = false
                selectedProduct = null
            },
            onSave = { product ->
                scope.launch {
                    if (selectedProduct == null) {
                        // Добавление нового товара
                        val result = FirebaseManager.addProduct(product)
                        result.onSuccess {
                            addProductLocally(product)
                            snackbarHostState.showSnackbar("Товар добавлен")
                            showAddDialog = false // Закрываем диалог
                        }.onFailure { e ->
                            snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                        }
                    } else {
                        // Обновление существующего
                        val result = FirebaseManager.updateProduct(product)
                        result.onSuccess {
                            updateProductLocally(product)
                            snackbarHostState.showSnackbar("Товар обновлен")
                            selectedProduct = null // Закрываем диалог
                        }.onFailure { e ->
                            snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun OrdersTab(
    snackbarHostState: SnackbarHostState
) {
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var showStatusDialog by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = FirebaseManager.getAllOrders()
        result.onSuccess { orderList ->
            orders = orderList
        }.onFailure { e ->
            snackbarHostState.showSnackbar("Ошибка загрузки: ${e.message}")
        }
        isLoading = false
    }

    fun updateOrderLocally(updatedOrder: Order) {
        orders = orders.map {
            if (it.id == updatedOrder.id) updatedOrder else it
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading && orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF7B00))
            }
        } else if (orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет заказов", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = orders,
                    key = { it.id }
                ) { order ->
                    AdminOrderCard(
                        order = order,
                        onStatusChange = { newStatus ->
                            scope.launch {
                                // Сначала обновляем UI
                                val updatedOrder = order.copy(status = newStatus)
                                updateOrderLocally(updatedOrder)

                                val result = FirebaseManager.adminUpdateOrderStatus(order.id, newStatus)
                                result.onFailure { e ->
                                    // Если ошибка, возвращаем старый статус
                                    updateOrderLocally(order)
                                    snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UsersTab(
    snackbarHostState: SnackbarHostState
) {
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var showDeleteUserDialog by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = FirebaseManager.getAllUsers()
        result.onSuccess { userList ->
            users = userList
        }.onFailure { e ->
            snackbarHostState.showSnackbar("Ошибка загрузки: ${e.message}")
        }
        isLoading = false
    }

    fun updateUserLocally(updatedUser: UserProfile) {
        users = users.map {
            if (it.id == updatedUser.id) updatedUser else it
        }
    }

    fun removeUserLocally(userId: String) {
        users = users.filter { it.id != userId }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading && users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF7B00))
            }
        } else if (users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет пользователей", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = users,
                    key = { it.id }
                ) { user ->
                    AdminUserCard(
                        user = user,
                        onRoleChange = { newRole ->
                            scope.launch {
                                // Сначала обновляем UI
                                val updatedUser = user.copy(role = newRole)
                                updateUserLocally(updatedUser)

                                val result = FirebaseManager.updateUserRole(user.id, newRole)
                                result.onFailure { e ->
                                    // Если ошибка, возвращаем старую роль
                                    updateUserLocally(user)
                                    snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                // Сначала обновляем UI
                                removeUserLocally(user.id)

                                val result = FirebaseManager.adminDeleteUser(user.id)
                                result.onFailure { e ->
                                    // Если ошибка, возвращаем пользователя обратно
                                    users = users + user
                                    snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatsTab() {
    var stats by remember { mutableStateOf<FirebaseManager.AdminStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = FirebaseManager.getAdminStats()
        result.onSuccess { adminStats ->
            stats = adminStats
        }
        isLoading = false
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF7B00))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    StatCard(
                        title = "Пользователи",
                        value = stats?.totalUsers?.toString() ?: "0",
                        icon = Icons.Default.People,
                        color = Color(0xFF3B82F6)
                    )
                }
                item {
                    StatCard(
                        title = "Товары",
                        value = stats?.totalProducts?.toString() ?: "0",
                        icon = Icons.Default.Inventory,
                        color = Color(0xFF10B981)
                    )
                }
                item {
                    StatCard(
                        title = "Всего заказов",
                        value = stats?.totalOrders?.toString() ?: "0",
                        icon = Icons.Default.ShoppingCart,
                        color = Color(0xFFF59E0B)
                    )
                }
                item {
                    StatCard(
                        title = "Выполнено",
                        value = stats?.completedOrders?.toString() ?: "0",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF10B981)
                    )
                }
                item {
                    StatCard(
                        title = "В ожидании",
                        value = stats?.pendingOrders?.toString() ?: "0",
                        icon = Icons.Default.HourglassEmpty,
                        color = Color(0xFFF59E0B)
                    )
                }
                item {
                    StatCard(
                        title = "Выручка",
                        value = "${stats?.totalRevenue?.toInt() ?: 0} ₽",
                        icon = Icons.Default.AttachMoney,
                        color = Color(0xFF8B5CF6)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Текст
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }
    }
}

@Composable
fun AdminProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAvailability: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .animateContentSize(), // Добавляем анимацию
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
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛠️", fontSize = 32.sp)
                    }
                }
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = product.description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${product.price} ₽/день",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF7B00)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Статус с анимацией
                    Switch(
                        checked = product.available,
                        onCheckedChange = { onToggleAvailability() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color(0xFFEF4444),
                            checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.5f),
                            uncheckedTrackColor = Color(0xFFEF4444).copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Кнопки действий
            Column(
                horizontalAlignment = Alignment.End
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    onStatusChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColors = mapOf(
        "pending" to Color(0xFFF59E0B),
        "confirmed" to Color(0xFF3B82F6),
        "in_progress" to Color(0xFF8B5CF6),
        "completed" to Color(0xFF10B981),
        "cancelled" to Color(0xFFEF4444)
    )

    val statusNames = mapOf(
        "pending" to "Ожидание",
        "confirmed" to "Подтвержден",
        "in_progress" to "В процессе",
        "completed" to "Завершен",
        "cancelled" to "Отменен"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(), // Добавляем анимацию
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Верхняя строка
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = order.productName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Text(
                        text = "Заказ #${order.id.takeLast(6)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Статус
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColors[order.status]?.copy(alpha = 0.1f) ?: Color.Gray.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusNames[order.status] ?: order.status,
                        fontSize = 11.sp,
                        color = statusColors[order.status] ?: Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Детали заказа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Клиент: ${order.userId.take(6)}...",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Срок: ${order.days} ${getDayText(order.days)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "${order.totalPrice.toInt()} ₽",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF7B00)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопка изменения статуса
            Button(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = statusColors[order.status] ?: Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Изменить статус", color = Color.White)
            }

            // Выпадающее меню статусов с анимацией
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    listOf("pending", "confirmed", "in_progress", "completed", "cancelled").forEach { status ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onStatusChange(status)
                                    expanded = false
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            color = if (order.status == status)
                                statusColors[status]?.copy(alpha = 0.1f) ?: Color.Gray.copy(alpha = 0.1f)
                            else Color.Transparent
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(statusColors[status] ?: Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    statusNames[status] ?: status,
                                    fontWeight = if (order.status == status) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: UserProfile,
    onRoleChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(), // Добавляем анимацию
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Аватар
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Информация
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Text(
                    text = user.email,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Text(
                    text = "Заказов: ${user.totalOrders} | Потрачено: ${user.totalSpent} ₽",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                // Роль
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (user.role == "admin") Color(0xFFFF7B00) else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (user.role == "admin") "Администратор" else "Пользователь",
                        fontSize = 11.sp,
                        color = if (user.role == "admin") Color(0xFFFF7B00) else Color.Gray,
                        fontWeight = if (user.role == "admin") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Кнопки действий
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Кнопка изменения роли
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Изменить роль",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Кнопка удаления
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Выпадающее меню выбора роли с анимацией
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    listOf("user", "admin").forEach { role ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRoleChange(role)
                                    expanded = false
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            color = if (user.role == role)
                                Color(0xFFFF7B00).copy(alpha = 0.1f)
                            else Color.Transparent
                        ) {
                            Text(
                                if (role == "admin") "Администратор" else "Пользователь",
                                fontWeight = if (user.role == role) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Электроинструменты") }
    var imageUrl by remember { mutableStateOf(product?.imageUrl ?: "") }
    var available by remember { mutableStateOf(product?.available ?: true) }
    var isSaving by remember { mutableStateOf(false) } // Для блокировки кнопок во время сохранения

    // Состояние для выпадающего списка категорий
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Электроинструменты",
        "Ручные инструменты",
        "Садовая техника",
        "Строительное оборудование"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(650.dp), // Увеличил высоту еще больше
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Заголовок
                Text(
                    text = if (product == null) "Добавить товар" else "Редактировать товар",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Поле названия
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    enabled = !isSaving
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Поле описания
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 3,
                    enabled = !isSaving
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ряд с ценой и категорией
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Поле цены
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Цена") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        enabled = !isSaving
                    )

                    // Выбор категории
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            label = { Text("Категория") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSaving) {
                                    categoryExpanded = !categoryExpanded
                                },
                            shape = RoundedCornerShape(8.dp),
                            readOnly = true,
                            enabled = !isSaving,
                            trailingIcon = {
                                Icon(
                                    imageVector = if (categoryExpanded)
                                        Icons.Default.ArrowDropUp
                                    else
                                        Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable(enabled = !isSaving) {
                                        categoryExpanded = !categoryExpanded
                                    }
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .heightIn(max = 200.dp)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            cat,
                                            fontWeight = if (cat == category) FontWeight.Bold else FontWeight.Normal,
                                            color = if (cat == category) Color(0xFFFF7B00) else Color(0xFF1E293B)
                                        )
                                    },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    },
                                    enabled = !isSaving
                                )
                                if (cat != categories.last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        color = Color.LightGray,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Поле URL изображения
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL изображения") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    enabled = !isSaving
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Переключатель доступности
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Доступно для аренды",
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1E293B)
                    )
                    Switch(
                        checked = available,
                        onCheckedChange = { if (!isSaving) available = it },
                        enabled = !isSaving,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color.Gray
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSaving
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                val newProduct = Product(
                                    id = product?.id ?: "",
                                    name = name,
                                    description = description,
                                    price = price.toDoubleOrNull() ?: 0.0,
                                    category = category,
                                    imageUrl = imageUrl,
                                    available = available,
                                    rating = product?.rating ?: 5.0
                                )
                                onSave(newProduct)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF7B00),
                            disabledContainerColor = Color(0xFFFF7B00).copy(alpha = 0.5f)
                        ),
                        enabled = !isSaving && name.isNotBlank() && price.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Сохранить", color = Color.White)
                        }
                    }
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

// Добавляем анимацию для выпадающих меню
@Composable
fun AnimatedVisibility(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn() +
                androidx.compose.animation.slideInVertically(initialOffsetY = { -20 }),
        exit = androidx.compose.animation.fadeOut() +
                androidx.compose.animation.slideOutVertically(targetOffsetY = { -20 })
    ) {
        content()
    }
}