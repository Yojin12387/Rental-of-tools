package com.example.diplomki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val viewModel: AuthViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onRegisterSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A), // Темно-синий
                        Color(0xFF3B82F6), // Синий
                        Color(0xFF60A5FA)  // Светло-синий
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Иконка и заголовок
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFF7B00), // Яркий оранжевый
                                        Color(0xFFFFA726)  // Светлый оранжевый
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔧",
                            fontSize = 36.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Создать аккаунт",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFF1E3A8A), // Темно-синий
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Присоединяйтесь к RentTools",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF6B7280), // Серый
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Поле имени
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = {
                            Text(
                                "Ваше имя",
                                color = Color(0xFF1E3A8A) // Темно-синий
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Имя",
                                tint = Color(0xFFFF7B00) // Оранжевый
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF7B00), // Оранжевый
                            unfocusedBorderColor = Color(0xFFD1D5DB), // Серый
                            focusedLabelColor = Color(0xFFFF7B00), // Оранжевый
                            focusedTextColor = Color(0xFF1E3A8A), // Темно-синий
                            unfocusedTextColor = Color(0xFF6B7280) // Серый
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Поле email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = {
                            Text(
                                "Email",
                                color = Color(0xFF1E3A8A) // Темно-синий
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email",
                                tint = Color(0xFFFF7B00) // Оранжевый
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF7B00), // Оранжевый
                            unfocusedBorderColor = Color(0xFFD1D5DB), // Серый
                            focusedLabelColor = Color(0xFFFF7B00), // Оранжевый
                            focusedTextColor = Color(0xFF1E3A8A), // Темно-синий
                            unfocusedTextColor = Color(0xFF6B7280) // Серый
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Поле телефона
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = {
                            Text(
                                "Номер телефона",
                                color = Color(0xFF1E3A8A) // Темно-синий
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Телефон",
                                tint = Color(0xFFFF7B00) // Оранжевый
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF7B00), // Оранжевый
                            unfocusedBorderColor = Color(0xFFD1D5DB), // Серый
                            focusedLabelColor = Color(0xFFFF7B00), // Оранжевый
                            focusedTextColor = Color(0xFF1E3A8A), // Темно-синий
                            unfocusedTextColor = Color(0xFF6B7280) // Серый
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Поле пароля
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                "Пароль",
                                color = Color(0xFF1E3A8A) // Темно-синий
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Пароль",
                                tint = Color(0xFFFF7B00) // Оранжевый
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Скрыть пароль"
                                    else "Показать пароль",
                                    tint = Color(0xFFFF7B00) // Оранжевый
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF7B00), // Оранжевый
                            unfocusedBorderColor = Color(0xFFD1D5DB), // Серый
                            focusedLabelColor = Color(0xFFFF7B00), // Оранжевый
                            focusedTextColor = Color(0xFF1E3A8A), // Темно-синий
                            unfocusedTextColor = Color(0xFF6B7280) // Серый
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Подтверждение пароля
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = {
                            Text(
                                "Подтвердите пароль",
                                color = Color(0xFF1E3A8A) // Темно-синий
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Подтвердите пароль",
                                tint = Color(0xFFFF7B00) // Оранжевый
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Скрыть пароль"
                                    else "Показать пароль",
                                    tint = Color(0xFFFF7B00) // Оранжевый
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF7B00), // Оранжевый
                            unfocusedBorderColor = Color(0xFFD1D5DB), // Серый
                            focusedLabelColor = Color(0xFFFF7B00), // Оранжевый
                            focusedTextColor = Color(0xFF1E3A8A), // Темно-синий
                            unfocusedTextColor = Color(0xFF6B7280) // Серый
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Проверка совпадения паролей
                    if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Пароли не совпадают",
                            color = Color(0xFFDC2626), // Красный для ошибок
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Кнопка регистрации с оранжевым градиентом
                    Button(
                        onClick = {
                            if (password == confirmPassword && name.isNotEmpty() &&
                                email.isNotEmpty() && phone.isNotEmpty() &&
                                password.isNotEmpty()) {
                                viewModel.registerUser(email, password, name, phone)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = name.isNotEmpty() && email.isNotEmpty() &&
                                phone.isNotEmpty() && password.isNotEmpty() &&
                                confirmPassword.isNotEmpty() && password == confirmPassword && uiState !is AuthUiState.Loading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF7B00), // Яркий оранжевый
                                            Color(0xFFFFA726)  // Светлый оранжевый
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Создать аккаунт",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Сообщение об ошибке
                    if (uiState is AuthUiState.Error) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (uiState as AuthUiState.Error).message,
                            color = Color(0xFFDC2626), // Красный для ошибок
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Разделитель
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFD1D5DB) // Серый
                        )
                        Text(
                            text = "или",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFF6B7280), // Серый
                            style = MaterialTheme.typography.bodySmall
                        )
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFD1D5DB) // Серый
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Ссылка на вход
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Уже есть аккаунт?",
                            color = Color(0xFF6B7280) // Серый
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFFF7B00) // Оранжевый
                            )
                        ) {
                            Text(
                                "Войти",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF7B00) // Оранжевый
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}