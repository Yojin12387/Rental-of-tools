    // screens/ProfileScreen.kt
    package com.example.diplomki.screens
    
    import android.content.Intent
    import android.net.Uri
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.PaddingValues
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.width
    import androidx.compose.foundation.lazy.LazyColumn
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
    import androidx.compose.ui.graphics.vector.ImageVector
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.navigation.NavHostController
    import com.example.diplomki.FirebaseManager
    import com.example.diplomki.R
    import com.example.diplomki.models.UserProfile
    import kotlinx.coroutines.launch

    @Composable
    fun ProfileScreen(
        navController: NavHostController,
        onLogout: () -> Unit
    ) {
        var userProfile by remember { mutableStateOf<com.example.diplomki.UserProfile?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            FirebaseManager.getCurrentUserProfile(
                onSuccess = { profile ->
                    userProfile = profile
                    isLoading = false
                },
                onError = { error ->
                    errorMessage = error
                    isLoading = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Ошибка: $error")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                ProfileTopBar(
                    onBackClick = { navController.popBackStack() }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF7B00)
                            )
                        }
                    }

                    errorMessage != null -> {
                        ErrorState(
                            message = errorMessage ?: "Ошибка загрузки",
                            onRetry = {
                                isLoading = true
                                errorMessage = null
                                FirebaseManager.getCurrentUserProfile(
                                    onSuccess = { profile ->
                                        userProfile = profile
                                        isLoading = false
                                    },
                                    onError = { error ->
                                        errorMessage = error
                                        isLoading = false
                                    }
                                )
                            }
                        )
                    }

                    userProfile != null -> {
                        ProfileContent(
                            userProfile = userProfile!!,
                            onEditProfile = {
                                // Переходим на экран редактирования
                                navController.navigate("editProfile/${userProfile!!.id}")
                            },
                            onContactSupport = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@toolrental.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Поддержка ToolRental")
                                    putExtra(Intent.EXTRA_TEXT, "Здравствуйте! Я нуждаюсь в помощи по поводу...")
                                }
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                }
                            },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ProfileTopBar(onBackClick: () -> Unit) {
        TopAppBar(
            title = {
                Text(
                    text = "Мой профиль",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
    
    @Composable
    fun ErrorState(message: String, onRetry: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = "Ошибка",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ошибка загрузки профиля",
                color = MaterialTheme.colorScheme.error,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Повторить попытку")
            }
        }
    }
    
    @Composable
    fun ProfileContent(
        userProfile: com.example.diplomki.UserProfile,
        onEditProfile: () -> Unit,
        onContactSupport: () -> Unit,
        onLogout: () -> Unit
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProfileHeader(userProfile = userProfile)
            }
    
            item {
                UserInfoCard(userProfile = userProfile)
            }
    
            item {
                ActionButtons(
                    onEditProfile = onEditProfile,
                    onContactSupport = onContactSupport,
                    onLogout = onLogout
                )
            }
        }
    }
    
    @Composable
    fun ProfileHeader(userProfile: com.example.diplomki.UserProfile) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Аватар пользователя
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    // Временно используем стандартную иконку
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Аватар",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
    
                Spacer(modifier = Modifier.width(16.dp))
    
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = userProfile.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
    
                    Spacer(modifier = Modifier.height(4.dp))
    
                    Text(
                        text = userProfile.email,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
    
                    Spacer(modifier = Modifier.height(8.dp))
    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Рейтинг",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFB74D)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", userProfile.rating),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${userProfile.totalOrders} заказов",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    fun UserInfoCard(userProfile: com.example.diplomki.UserProfile) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Контактная информация",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
    
                Divider(modifier = Modifier.padding(bottom = 12.dp))
    
                InfoItem(
                    icon = Icons.Filled.Person,
                    title = "Имя",
                    value = userProfile.name
                )
    
                InfoItem(
                    icon = Icons.Filled.Email,
                    title = "Email",
                    value = userProfile.email
                )
    
                InfoItem(
                    icon = Icons.Filled.Phone,
                    title = "Телефон",
                    value = userProfile.phone
                )
    
                InfoItem(
                    icon = Icons.Filled.LocationOn,
                    title = "Адрес",
                    value = userProfile.address
                )
    
                InfoItem(
                    icon = Icons.Filled.CalendarToday,
                    title = "Дата регистрации",
                    value = userProfile.formattedDate
                )
    
                InfoItem(
                    icon = Icons.Filled.ShoppingCart,
                    title = "Всего заказов",
                    value = userProfile.totalOrders.toString()
                )
    
                InfoItem(
                    icon = Icons.Filled.AttachMoney,
                    title = "Всего потрачено",
                    value = String.format("%.2f ₽", userProfile.totalSpent)
                )
            }
        }
    }
    
    @Composable
    fun InfoItem(
        icon: ImageVector,
        title: String,
        value: String
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
    
    @Composable
    fun ActionButtons(
        onEditProfile: () -> Unit,
        onContactSupport: () -> Unit,
        onLogout: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onEditProfile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Редактировать профиль")
                }
    
                OutlinedButton(
                    onClick = onContactSupport,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Support,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Служба поддержки")
                }
    
                Divider(modifier = Modifier.padding(vertical = 8.dp))
    
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выйти из аккаунта")
                }
    
                Text(
                    text = "После выхода вы сможете войти снова, используя email и пароль",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }