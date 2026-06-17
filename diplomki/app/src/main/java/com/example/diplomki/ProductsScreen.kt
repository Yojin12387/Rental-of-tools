package com.example.diplomki.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import com.example.diplomki.FirebaseManager
import com.example.diplomki.Product
import kotlinx.coroutines.launch
import com.example.diplomki.AppColors



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    navController: NavHostController,
    onProductClick: (Product) -> Unit,
    onOrdersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Все") }
    var sortOption by remember { mutableStateOf("🔥 Популярные") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Состояние для товаров из Firebase
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val categories = listOf(
        "Все",
        "🔧 Электро",
        "⚒️ Ручные",
        "🌿 Сад",
        "🏗️ Строит."
    )
    val sortOptions = listOf("🔥 Популярные", "💰 Цена ↑", "💰 Цена ↓", "⭐ Рейтинг", "📝 Название")

    // Загрузка товаров из Firebase
    LaunchedEffect(Unit) {
        FirebaseManager.getProductsStream().collect { productList ->
            products = productList
            isLoading = false
        }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory, sortOption) {
        products.filter { product ->
            val categoryMatch = when (selectedCategory) {
                "Все" -> true
                "🔧 Электро" -> product.category == "Электроинструменты"
                "⚒️ Ручные" -> product.category == "Ручные инструменты"
                "🌿 Сад" -> product.category == "Садовая техника"
                "🏗️ Строит." -> product.category == "Строительное оборудование"
                else -> product.category == selectedCategory
            }
            categoryMatch && (searchQuery.isEmpty() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.description.contains(searchQuery, ignoreCase = true))
        }.sortedWith(when (sortOption) {
            "💰 Цена ↑" -> compareBy { it.price }
            "💰 Цена ↓" -> compareByDescending { it.price }
            "⭐ Рейтинг" -> compareByDescending { it.rating }
            "📝 Название" -> compareBy { it.name }
            else -> compareByDescending { it.rating }
        })
    }

    // Диалог выхода
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = AppColors.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Выход из аккаунта")
            },
            text = {
                Text("Вы действительно хотите выйти?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        scope.launch {
                            FirebaseManager.logout()
                            onLogout()
                        }
                    }
                ) {
                    Text("Выйти")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = AppColors.White,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                // Профиль в меню
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(AppColors.DeepBlue, AppColors.ElectricBlue)
                            )
                        )
                        .padding(vertical = 40.dp, horizontal = 20.dp)
                ) {
                    Column {
                        // Аватар
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AppColors.GlassWhite)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = AppColors.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Меню",
                            color = AppColors.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "Аренда инструментов",
                            color = AppColors.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }

                // Пункты меню с иконками
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = AppColors.DeepBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Профиль",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.DarkNavy
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            val isAdmin = FirebaseManager.isCurrentUserAdmin()
                            if (isAdmin) {
                                navController.navigate("admin")
                            } else {
                                navController.navigate("profile")
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = AppColors.LightGray,
                    thickness = 1.dp
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = AppColors.DeepBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Корзина",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.DarkNavy
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOrdersClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                // ✅ ИЗБРАННОЕ (ОДИН РАЗ)
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = AppColors.DeepBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Избранное",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.DarkNavy
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("favorites")
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = AppColors.DeepBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "История аренды",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.DarkNavy
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("orders")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )



                Spacer(modifier = Modifier.weight(1f))

                // Выход
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = AppColors.Error,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Выйти",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.Error
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showLogoutDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                // Collapsing TopAppBar с кнопкой меню слева
                LargeTopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Заголовок (скрывается при скролле)
                            Text(
                                text = "Добро пожаловать!",
                                color = AppColors.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Text(
                                text = "Аренда инструментов",
                                color = AppColors.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        offset = Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                ),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    },
                    navigationIcon = {
                        // Кнопка меню слева
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppColors.GlassWhite)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Меню",
                                tint = AppColors.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AppColors.DarkNavy,
                                AppColors.Navy,
                                AppColors.DeepBlue
                            )
                        )
                    )
                    .padding(paddingValues)
            ) {
                // Фоновые декоративные элементы
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 100.dp, y = (-50).dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AppColors.ElectricBlue.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(400.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-100).dp, y = 100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AppColors.Orange.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Основной контент
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Поиск
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AppColors.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 8.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Поиск",
                                    tint = AppColors.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = {
                                        Text(
                                            text = "Поиск инструментов...",
                                            color = AppColors.Gray,
                                            fontSize = 15.sp
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = AppColors.DarkNavy,
                                        unfocusedTextColor = AppColors.DarkNavy
                                    )
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Очистить",
                                            tint = AppColors.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Категории
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = {
                                        Text(
                                            text = category,
                                            fontSize = 14.sp,
                                            fontWeight = if (selectedCategory == category)
                                                FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppColors.Orange,
                                        selectedLabelColor = AppColors.White,
                                        containerColor = AppColors.White,
                                        labelColor = AppColors.DarkNavy
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = null
                                )
                            }
                        }
                    }

                    // Сортировка и статистика
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Статистика
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.Orange)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Найдено: ${filteredProducts.size}",
                                    color = AppColors.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }

                            // Кнопка сортировки
                            var sortExpanded by remember { mutableStateOf(false) }

                            Box {
                                Button(
                                    onClick = { sortExpanded = true },
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.White
                                    ),
                                    shape = RoundedCornerShape(30.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            when {
                                                sortOption.contains("🔥") -> {
                                                    Icon(
                                                        Icons.Default.Whatshot,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = AppColors.Orange
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Популярные",
                                                        fontSize = 12.sp,
                                                        color = AppColors.DarkNavy,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                sortOption.contains("💰") && sortOption.contains("↑") -> {
                                                    Icon(
                                                        Icons.Default.TrendingUp,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = AppColors.Orange
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Цена ↑",
                                                        fontSize = 12.sp,
                                                        color = AppColors.DarkNavy
                                                    )
                                                }
                                                sortOption.contains("💰") && sortOption.contains("↓") -> {
                                                    Icon(
                                                        Icons.Default.TrendingDown,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = AppColors.Orange
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Цена ↓",
                                                        fontSize = 12.sp,
                                                        color = AppColors.DarkNavy
                                                    )
                                                }
                                                sortOption.contains("⭐") -> {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = AppColors.Orange
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Рейтинг",
                                                        fontSize = 12.sp,
                                                        color = AppColors.DarkNavy
                                                    )
                                                }
                                                sortOption.contains("📝") -> {
                                                    Icon(
                                                        Icons.Default.SortByAlpha,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = AppColors.Orange
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Название",
                                                        fontSize = 12.sp,
                                                        color = AppColors.DarkNavy
                                                    )
                                                }
                                            }
                                        }

                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = AppColors.Gray
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = sortExpanded,
                                    onDismissRequest = { sortExpanded = false },
                                    modifier = Modifier
                                        .width(160.dp)
                                        .background(
                                            color = AppColors.White,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    sortOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option,
                                                    fontSize = 14.sp,
                                                    color = if (option == sortOption) AppColors.Orange else AppColors.DarkNavy,
                                                    fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                sortOption = option
                                                sortExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Товары
                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = AppColors.White,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Загрузка товаров...",
                                        color = AppColors.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else if (filteredProducts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier.padding(32.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = AppColors.GlassWhite
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        Text(
                                            text = if (products.isEmpty()) "🛠️" else "🔍",
                                            fontSize = 64.sp
                                        )
                                        Text(
                                            text = if (products.isEmpty())
                                                "Товары не найдены"
                                            else
                                                "По вашему запросу ничего не найдено",
                                            color = AppColors.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        if (searchQuery.isNotEmpty() || selectedCategory != "Все") {
                                            Button(
                                                onClick = {
                                                    searchQuery = ""
                                                    selectedCategory = "Все"
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AppColors.Orange
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(text = "Сбросить фильтры", color = AppColors.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(
                            items = filteredProducts,
                            key = { it.id }
                        ) { product ->
                            ProductCard(
                                product = product,
                                onClick = { onProductClick(product) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SortMenuItem(
    option: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) AppColors.Orange else AppColors.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = option,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AppColors.Orange else AppColors.DarkNavy,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = AppColors.Orange
                    )
                }
            }
        },
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false
            ),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Изображение товара
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AppColors.LightGray,
                                AppColors.OffWhite
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AppColors.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = AppColors.DeepBlue,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AppColors.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🔧",
                                    fontSize = 48.sp
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppColors.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔧",
                            fontSize = 48.sp
                        )
                    }
                }

                // Статус доступности
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = if (product.available) AppColors.Success else AppColors.Error,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = if (product.available) "✓ Доступно" else "✗ Занято",
                        color = AppColors.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Информация о товаре
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = product.name,
                        fontSize = 18.sp,
                        color = AppColors.DarkNavy,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.3.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = product.description,
                        fontSize = 13.sp,
                        color = AppColors.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Цена за сутки",
                            fontSize = 11.sp,
                            color = AppColors.Gray
                        )
                        Text(
                            text = "${product.price.toInt()} ₽",
                            fontSize = 22.sp,
                            color = AppColors.Orange,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppColors.Orange.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format("%.1f", product.rating),
                                fontSize = 14.sp,
                                color = AppColors.Orange,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Рейтинг",
                                tint = AppColors.Orange,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}