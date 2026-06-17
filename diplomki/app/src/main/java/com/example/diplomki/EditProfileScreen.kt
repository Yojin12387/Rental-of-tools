// screens/EditProfileScreen.kt
package com.example.diplomki.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.diplomki.FirebaseManager
import com.example.diplomki.models.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavHostController,
    currentUserProfile: UserProfile
) {
    var name by remember { mutableStateOf(currentUserProfile.name) }
    var phone by remember { mutableStateOf(currentUserProfile.phone) }
    var address by remember { mutableStateOf(currentUserProfile.address) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isFormValid = name.isNotBlank() && phone.isNotBlank()

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            EditProfileTopBar(
                onBackClick = { navController.popBackStack() }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Email (только для чтения)
            OutlinedTextField(
                value = currentUserProfile.email,
                onValueChange = {},
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Filled.Email, null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Имя
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя *") },
                leadingIcon = {
                    Icon(Icons.Filled.Person, null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = name.isEmpty(),
                supportingText = {
                    if (name.isEmpty()) {
                        Text("Введите имя")
                    }
                }
            )

            // Телефон
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Телефон *") },
                leadingIcon = {
                    Icon(Icons.Filled.Phone, null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                isError = phone.isEmpty(),
                supportingText = {
                    if (phone.isEmpty()) {
                        Text("Введите телефон")
                    }
                }
            )

            // Адрес
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Адрес") },
                leadingIcon = {
                    Icon(Icons.Filled.LocationOn, null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )

            // Статистика (только чтение)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Статистика",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentUserProfile.totalOrders.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Заказов",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format("%.2f ₽", currentUserProfile.totalSpent),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Потрачено",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format("%.1f", currentUserProfile.rating),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Рейтинг",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка сохранения
            Button(
                onClick = {
                    if (!isFormValid) {
                        errorMessage = "Заполните обязательные поля"
                        return@Button
                    }

                    isLoading = true
                    FirebaseManager.updateUserProfile(
                        name = name,
                        phone = phone,
                        address = address,
                        onSuccess = {
                            isLoading = false
                            successMessage = "Профиль успешно обновлен"
                            scope.launch {
                                snackbarHostState.showSnackbar("✅ Профиль обновлен")
                            }
                        },
                        onError = { error ->
                            isLoading = false
                            errorMessage = error
                            scope.launch {
                                snackbarHostState.showSnackbar("❌ Ошибка: $error")
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && isFormValid,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранение...")
                } else {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранить изменения")
                }
            }

            // Кнопка отмены
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text("Отмена")
            }

            // Сообщения об ошибках
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "Ошибка",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Редактирование профиля",
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