package com.example.diplomki

import com.example.diplomki.models.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.firestore.Query
import com.example.diplomki.bd.CartProduct



// Модель профиля пользователя
data class UserProfile(
    val id: String = "",
    val email: String = "",
    val name: String = "Пользователь",
    val phone: String = "Не указан",
    val address: String = "Не указан",
    val createdAt: String = "",
    val avatarUrl: String? = null,
    val totalOrders: Int = 0,
    val totalSpent: Double = 0.0,
    val rating: Double = 0.0,
    val role: String = "user"
) {
    val formattedDate: String
        get() = try {
            val date = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH).parse(createdAt)
            if (date != null) {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)
            } else {
                "Неизвестно"
            }
        } catch (e: Exception) {
            createdAt
        }
}


object FirebaseManager {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth

    // Коллекции Firestore
    private const val PRODUCTS_COLLECTION = "products"
    private const val ORDERS_COLLECTION = "orders"
    private const val USERS_COLLECTION = "users"
    private const val REVIEWS_COLLECTION = "reviews"
    private const val CART_COLLECTION = "cart"
    // Список email админов
    private val adminEmails = listOf(
        "timurbiktasev134@gmail.com", // замените на свой email
        "admin@example.com"
    )

    // Текущий пользователь
    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    // ✅ Проверка прав администратора
    suspend fun isCurrentUserAdmin(): Boolean {
        val currentUser = auth.currentUser ?: return false

        // Сначала проверяем по email из списка
        if (currentUser.email in adminEmails) {
            return true
        }

        // Если нет в списке, проверяем роль в Firestore
        return try {
            val userId = currentUser.uid
            val document = db.collection(USERS_COLLECTION).document(userId).get().await()
            document.getString("role") == "admin"
        } catch (e: Exception) {
            false
        }
    }

    // ✅ Получение роли пользователя
    suspend fun getUserRole(): String {
        return if (isCurrentUserAdmin()) "admin" else "user"
    }

    // ✅ Обновление роли пользователя (для админов)
    suspend fun updateUserRole(userId: String, newRole: String): Result<Boolean> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(Exception("Требуются права администратора"))
            }

            db.collection(USERS_COLLECTION).document(userId)
                .update("role", newRole).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ АДМИН: Добавление товара
    suspend fun addProduct(product: Product): Result<Boolean> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(Exception("Требуются права администратора"))
            }

            val productWithId = if (product.id.isEmpty()) {
                product.copy(id = db.collection(PRODUCTS_COLLECTION).document().id)
            } else product

            // Преобразуем Product в Map для сохранения
            val productData = hashMapOf(
                "id" to productWithId.id,
                "name" to productWithId.name,
                "description" to productWithId.description,
                "price" to productWithId.price,
                "imageUrl" to productWithId.imageUrl,
                "category" to productWithId.category,
                "rating" to productWithId.rating,
                "available" to productWithId.available,
                "characteristics" to productWithId.characteristics
            )

            db.collection(PRODUCTS_COLLECTION)
                .document(productWithId.id)
                .set(productData)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ АДМИН: Обновление товара
    suspend fun updateProduct(product: Product): Result<Boolean> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(Exception("Требуются права администратора"))
            }

            val productData = hashMapOf(
                "id" to product.id,
                "name" to product.name,
                "description" to product.description,
                "price" to product.price,
                "imageUrl" to product.imageUrl,
                "category" to product.category,
                "rating" to product.rating,
                "available" to product.available,
                "characteristics" to product.characteristics
            )

            db.collection(PRODUCTS_COLLECTION)
                .document(product.id)
                .set(productData)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ АДМИН: Удаление товара
    suspend fun deleteProduct(productId: String): Result<Boolean> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(Exception("Требуются права администратора"))
            }

            db.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .delete()
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ АДМИН: Обновление доступности товара
    suspend fun updateProductAvailability(productId: String, available: Boolean): Result<Boolean> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(Exception("Требуются права администратора"))
            }

            db.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .update("available", available)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Метод для пользователей - обновление доступности при заказе
    suspend fun markProductAsBusy(productId: String): Boolean {
        return try {
            db.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .update("available", false)
                .await()
            true
        } catch (e: Exception) {
            println("❌ Ошибка при обновлении статуса товара: ${e.message}")
            false
        }
    }

    // Или с Result для единообразия
    suspend fun markProductAsBusyResult(productId: String): Result<Boolean> {
        return try {
            db.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .update("available", false)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ АДМИН: Получение всех пользователей
    suspend fun getAllUsers(): Result<List<UserProfile>> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(Exception("Требуются права администратора"))
            }

            val snapshot = db.collection(USERS_COLLECTION).get().await()
            val users = snapshot.documents.mapNotNull { document ->
                try {
                    UserProfile(
                        id = document.id,
                        email = document.getString("email") ?: "",
                        name = document.getString("name") ?: "Пользователь",
                        phone = document.getString("phone") ?: "Не указан",
                        address = document.getString("address") ?: "Не указан",
                        createdAt = document.getString("createdAt") ?: "",
                        avatarUrl = document.getString("avatarUrl"),
                        totalOrders = document.getLong("totalOrders")?.toInt() ?: 0,
                        totalSpent = document.getDouble("totalSpent") ?: 0.0,
                        rating = document.getDouble("rating") ?: 0.0,
                        role = document.getString("role") ?: "user"
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ АДМИН: Получение всех заказов
    suspend fun getAllOrders(): Result<List<Order>> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(Exception("Требуются права администратора"))
            }

            val snapshot = db.collection(ORDERS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = snapshot.documents.mapNotNull { document ->
                try {
                    Order(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        productId = document.getString("productId") ?: "",
                        productName = document.getString("productName") ?: "",
                        days = document.getLong("days")?.toInt() ?: 0,
                        totalPrice = document.getDouble("totalPrice") ?: 0.0,
                        status = document.getString("status") ?: "pending",
                        createdAt = document.getString("createdAt") ?: "",
                        startDate = document.getString("startDate") ?: "",
                        endDate = document.getString("endDate") ?: "",
                        userPhone = document.getString("userPhone") ?: "",
                        userAddress = document.getString("userAddress") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ АДМИН: Обновление статуса заказа
    suspend fun adminUpdateOrderStatus(orderId: String, status: String): Result<Boolean> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(Exception("Требуются права администратора"))
            }

            db.collection(ORDERS_COLLECTION)
                .document(orderId)
                .update("status", status)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ АДМИН: Удаление пользователя
    suspend fun adminDeleteUser(userId: String): Result<Boolean> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(Exception("Требуются права администратора"))
            }

            // Удаляем данные пользователя из Firestore
            db.collection(USERS_COLLECTION).document(userId).delete().await()

            // Удаляем все заказы пользователя
            val ordersSnapshot = db.collection(ORDERS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            ordersSnapshot.documents.forEach { order ->
                order.reference.delete().await()
            }

            // Удаляем корзину пользователя
            val cartSnapshot = db.collection(CART_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            cartSnapshot.documents.forEach { cart ->
                cart.reference.delete().await()
            }

            // Удаляем отзывы пользователя
            val reviewsSnapshot = db.collection(REVIEWS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            reviewsSnapshot.documents.forEach { review ->
                review.reference.delete().await()
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ АДМИН: Получение статистики
    suspend fun getAdminStats(): Result<AdminStats> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(Exception("Требуются права администратора"))
            }

            val usersCount = db.collection(USERS_COLLECTION).get().await().size()
            val productsCount = db.collection(PRODUCTS_COLLECTION).get().await().size()

            val orders = getAllOrders().getOrNull() ?: emptyList()
            val totalOrders = orders.size
            val completedOrders = orders.count { it.status == "completed" }
            val pendingOrders = orders.count { it.status == "pending" }

            val totalRevenue = orders.filter { it.status == "completed" }
                .sumOf { it.totalPrice }

            Result.success(AdminStats(
                totalUsers = usersCount,
                totalProducts = productsCount,
                totalOrders = totalOrders,
                completedOrders = completedOrders,
                pendingOrders = pendingOrders,
                totalRevenue = totalRevenue
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ Проверка состояния аутентификации с колбэком
    fun checkAuthState(onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        val isLoggedIn = currentUser != null
        onComplete(isLoggedIn)

        // Также подписываемся на изменения состояния аутентификации
        auth.addAuthStateListener { firebaseAuth ->
            // Обновляем состояние, если нужно
        }
    }

    // Регистрация пользователя (обновленная с ролью)
    suspend fun registerUser(email: String, password: String, name: String, phone: String): Boolean {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return false

            // Определяем роль (админ если email в списке)
            val role = if (email in adminEmails) "admin" else "user"

            // Создаем документ пользователя в Firestore
            val userData = hashMapOf(
                "name" to name,
                "email" to email,
                "phone" to phone,
                "address" to "",
                "createdAt" to Date().toString(),
                "favorites" to emptyList<String>(),
                "totalOrders" to 0,
                "totalSpent" to 0.0,
                "rating" to 0.0,
                "role" to role // Добавляем роль
            )

            db.collection(USERS_COLLECTION).document(userId).set(userData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Получить данные текущего пользователя (обновленная)
    fun getCurrentUserProfile(
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError("Пользователь не авторизован")
            return
        }

        val userId = currentUser.uid

        db.collection(USERS_COLLECTION).document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val totalOrders = document.getLong("totalOrders")?.toInt() ?: 0
                    val totalSpent = document.getDouble("totalSpent") ?: 0.0
                    val rating = document.getDouble("rating") ?: 0.0
                    val role = document.getString("role") ?:
                    if (currentUser.email in adminEmails) "admin" else "user"

                    val userProfile = UserProfile(
                        id = userId,
                        email = currentUser.email ?: "",
                        name = document.getString("name") ?: "Пользователь",
                        phone = document.getString("phone") ?: "Не указан",
                        address = document.getString("address") ?: "Не указан",
                        createdAt = document.getString("createdAt") ?: "",
                        avatarUrl = null,
                        totalOrders = totalOrders,
                        totalSpent = totalSpent,
                        rating = rating,
                        role = role
                    )
                    onSuccess(userProfile)
                } else {
                    // Если документа нет, создаем его
                    val role = if (currentUser.email in adminEmails) "admin" else "user"

                    val newUserProfile = UserProfile(
                        id = userId,
                        email = currentUser.email ?: "",
                        name = currentUser.displayName ?: "Пользователь",
                        phone = "",
                        address = "",
                        createdAt = Date().toString(),
                        avatarUrl = null,
                        totalOrders = 0,
                        totalSpent = 0.0,
                        rating = 0.0,
                        role = role
                    )

                    // Сохраняем в Firestore
                    val userData = hashMapOf(
                        "name" to newUserProfile.name,
                        "email" to newUserProfile.email,
                        "phone" to newUserProfile.phone,
                        "address" to newUserProfile.address,
                        "createdAt" to newUserProfile.createdAt,
                        "favorites" to emptyList<String>(),
                        "totalOrders" to newUserProfile.totalOrders,
                        "totalSpent" to newUserProfile.totalSpent,
                        "rating" to newUserProfile.rating,
                        "role" to newUserProfile.role
                    )

                    db.collection(USERS_COLLECTION).document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            onSuccess(newUserProfile)
                        }
                        .addOnFailureListener { e ->
                            onError("Ошибка создания профиля: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                onError("Ошибка загрузки профиля: ${e.message}")
            }
    }

    // Вход пользователя
    suspend fun loginUser(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Выход пользователя
    fun logout() {
        auth.signOut()
    }



    // Обновить профиль пользователя
    fun updateUserProfile(
        name: String,
        phone: String,
        address: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError("Пользователь не авторизован")
            return
        }

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "address" to address
        )

        db.collection(USERS_COLLECTION).document(currentUser.uid)
            .update(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError("Ошибка обновления: ${e.message}")
            }
    }

    // Получить все товары
    fun getProductsStream(): Flow<List<Product>> = flow {
        try {
            val querySnapshot = db.collection(PRODUCTS_COLLECTION).get().await()
            val products = querySnapshot.documents.mapNotNull { document ->
                try {
                    // Получаем характеристики как Map
                    val characteristicsMap = document.get("characteristics") as? Map<*, *>
                    val characteristics = mutableMapOf<String, String>()

                    characteristicsMap?.forEach { (key, value) ->
                        if (key is String && value is String) {
                            characteristics[key] = value
                        }
                    }

                    Product(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        description = document.getString("description") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        imageUrl = document.getString("imageUrl") ?: "",
                        category = document.getString("category") ?: "",
                        rating = document.getDouble("rating") ?: 0.0,
                        available = document.getBoolean("available") ?: false,
                        characteristics = characteristics
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            println("✅ Загружено ${products.size} товаров из Firebase")
            emit(products)
        } catch (e: Exception) {
            println("❌ Ошибка при загрузке товаров: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    // Получить товары по категории
    suspend fun getProductsByCategory(category: String): List<Product> {
        return try {
            val querySnapshot = db.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("category", category)
                .get().await()
            querySnapshot.documents.mapNotNull { document ->
                try {
                    val characteristicsMap = document.get("characteristics") as? Map<*, *>
                    val characteristics = mutableMapOf<String, String>()

                    characteristicsMap?.forEach { (key, value) ->
                        if (key is String && value is String) {
                            characteristics[key] = value
                        }
                    }

                    Product(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        description = document.getString("description") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        imageUrl = document.getString("imageUrl") ?: "",
                        category = document.getString("category") ?: "",
                        rating = document.getDouble("rating") ?: 0.0,
                        available = document.getBoolean("available") ?: false,
                        characteristics = characteristics
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Получить товар по ID
    suspend fun getProductById(productId: String): Product? {
        return try {
            val document = db.collection(PRODUCTS_COLLECTION).document(productId).get().await()
            if (document.exists()) {
                val characteristicsMap = document.get("characteristics") as? Map<*, *>
                val characteristics = mutableMapOf<String, String>()

                characteristicsMap?.forEach { (key, value) ->
                    if (key is String && value is String) {
                        characteristics[key] = value
                    }
                }

                Product(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    description = document.getString("description") ?: "",
                    price = document.getDouble("price") ?: 0.0,
                    imageUrl = document.getString("imageUrl") ?: "",
                    category = document.getString("category") ?: "",
                    rating = document.getDouble("rating") ?: 0.0,
                    available = document.getBoolean("available") ?: false,
                    characteristics = characteristics
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    // Создать заказ
    suspend fun createOrder(
        productId: String,
        productName: String,
        productImage: String = "",
        days: Int,
        totalPrice: Double,
        startDate: String,
        endDate: String,
        userPhone: String = "",
        userAddress: String = ""
    ): String? {
        return try {
            val userId = auth.currentUser?.uid ?: return null

            println("📝 СОЗДАНИЕ ЗАКАЗА:")
            println("📝 userId: $userId") // Проверьте, что это правильный ID

            val orderData = hashMapOf(
                "userId" to userId, // ВАЖНО: именно "userId", а не "userld"!
                "productId" to productId,
                "productName" to productName,
                "productImage" to productImage,
                "days" to days,
                "totalPrice" to totalPrice,
                "status" to "pending",
                "createdAt" to Date().toString(),
                "startDate" to startDate,
                "endDate" to endDate,
                "userPhone" to userPhone,
                "userAddress" to userAddress
            )

            println("📝 Данные заказа: $orderData")

            val documentReference = db.collection(ORDERS_COLLECTION).add(orderData).await()
            println("✅ Заказ создан с ID: ${documentReference.id}")
            documentReference.id
        } catch (e: Exception) {
            println("❌ Ошибка при создании заказа: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // Получить заказы пользователя (упрощенная версия)
    // Получить заказы пользователя
    fun getUserOrders(): Flow<List<Order>> = flow {
        try {
            val userId = auth.currentUser?.uid ?: run {
                emit(emptyList())
                return@flow
            }

            println("📦 Загрузка заказов для userId: $userId")

            val snapshot = db.collection(ORDERS_COLLECTION)
                .whereEqualTo("userId", userId) // ВАЖНО: ищем по "userId"
                .get()
                .await()

            println("📦 Найдено документов: ${snapshot.documents.size}")

            if (snapshot.documents.isEmpty()) {
                println("📦 Нет заказов для userId: $userId")
            } else {
                snapshot.documents.forEach { doc ->
                    println("📄 Документ: ${doc.id}")
                    println("📄 Данные: ${doc.data}")
                }
            }

            val orders = snapshot.documents.mapNotNull { doc ->
                try {
                    Order(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        productId = doc.getString("productId") ?: "",
                        productName = doc.getString("productName") ?: "Без названия",
                        productImage = doc.getString("productImage") ?: "",
                        days = (doc.get("days") as? Long)?.toInt() ?: 0,
                        totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                        status = doc.getString("status") ?: "pending",
                        createdAt = doc.getString("createdAt") ?: "",
                        startDate = doc.getString("startDate") ?: "",
                        endDate = doc.getString("endDate") ?: "",
                        userPhone = doc.getString("userPhone") ?: "",
                        userAddress = doc.getString("userAddress") ?: ""
                    )
                } catch (e: Exception) {
                    println("❌ Ошибка парсинга: ${e.message}")
                    null
                }
            }

            println("✅ Загружено заказов: ${orders.size}")
            emit(orders)
        } catch (e: Exception) {
            println("❌ Ошибка загрузки: ${e.message}")
            emit(emptyList())
        }
    }
    // Получить заказы пользователя
    fun getUserOrdersStream(): Flow<List<Order>> = flow {
        try {
            val userId = auth.currentUser?.uid
            println("📦 ЗАГРУЗКА ЗАКАЗОВ:")
            println("📦 userId: $userId")

            if (userId == null) {
                println("📦 userId = null, пользователь не авторизован")
                emit(emptyList())
                return@flow
            }

            val querySnapshot = db.collection(ORDERS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            println("📦 Найдено документов в Firestore: ${querySnapshot.documents.size}")

            if (querySnapshot.documents.isEmpty()) {
                println("📦 В Firestore нет документов для userId: $userId")
            } else {
                querySnapshot.documents.forEachIndexed { index, document ->
                    println("📄 Документ $index: ${document.id}")
                    println("📄 Данные: ${document.data}")
                }
            }

            val orders = querySnapshot.documents.mapNotNull { document ->
                try {
                    Order(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        productId = document.getString("productId") ?: "",
                        productName = document.getString("productName") ?: "Без названия",
                        productImage = document.getString("productImage") ?: "",
                        days = document.getLong("days")?.toInt() ?: 0,
                        totalPrice = document.getDouble("totalPrice") ?: 0.0,
                        status = document.getString("status") ?: "pending",
                        createdAt = document.getString("createdAt") ?: "",
                        startDate = document.getString("startDate") ?: "",
                        endDate = document.getString("endDate") ?: "",
                        userPhone = document.getString("userPhone") ?: "",
                        userAddress = document.getString("userAddress") ?: ""
                    ).also {
                        println("✅ Заказ преобразован: ${it.productName}")
                    }
                } catch (e: Exception) {
                    println("❌ Ошибка парсинга заказа: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }

            println("📦 ИТОГО загружено заказов: ${orders.size}")
            emit(orders)
        } catch (e: Exception) {
            println("❌ Ошибка при загрузке заказов: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    // Обновить статус заказа
    suspend fun updateOrderStatus(orderId: String, status: String): Boolean {
        return try {
            db.collection(ORDERS_COLLECTION).document(orderId)
                .update("status", status).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Получить избранные товары пользователя
    suspend fun getUserFavorites(): List<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()
            val document = db.collection(USERS_COLLECTION).document(userId).get().await()
            (document.get("favorites") as? List<String>) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Добавить/удалить из избранного
    suspend fun toggleFavorite(productId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val userDoc = db.collection(USERS_COLLECTION).document(userId)

            val currentFavorites = getUserFavorites().toMutableSet()

            if (currentFavorites.contains(productId)) {
                currentFavorites.remove(productId)
            } else {
                currentFavorites.add(productId)
            }

            userDoc.update("favorites", currentFavorites.toList()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Проверить, есть ли товар в избранном
    suspend fun isFavorite(productId: String): Boolean {
        return getUserFavorites().contains(productId)
    }

    // Получить избранные товары
    suspend fun getFavoriteProducts(): List<Product> {
        return try {
            val favoriteIds = getUserFavorites()
            if (favoriteIds.isEmpty()) return emptyList()

            val products = mutableListOf<Product>()
            for (productId in favoriteIds) {
                val product = getProductById(productId)
                if (product != null) {
                    products.add(product)
                }
            }
            products
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Получить отзывы товара
    suspend fun getProductReviews(productId: String): List<Review> {
        return try {
            val querySnapshot = db.collection(REVIEWS_COLLECTION)
                .whereEqualTo("productId", productId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    Review(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        userName = document.getString("userName") ?: "",
                        productId = document.getString("productId") ?: "",
                        rating = document.getDouble("rating") ?: 0.0,
                        comment = document.getString("comment") ?: "",
                        createdAt = document.getString("createdAt") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Добавить отзыв
    suspend fun addReview(productId: String, rating: Double, comment: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val userName = auth.currentUser?.email ?: "Аноним"

            val reviewData = hashMapOf(
                "userId" to userId,
                "userName" to userName,
                "productId" to productId,
                "rating" to rating,
                "comment" to comment,
                "createdAt" to Date().toString()
            )

            db.collection(REVIEWS_COLLECTION).add(reviewData).await()

            // Обновить рейтинг товара
            updateProductRating(productId, rating)

            true
        } catch (e: Exception) {
            false
        }
    }

    // Обновить рейтинг товара
    private suspend fun updateProductRating(productId: String, newRating: Double) {
        try {
            val reviews = getProductReviews(productId)
            if (reviews.isNotEmpty()) {
                val averageRating = (reviews.sumOf { it.rating } + newRating) / (reviews.size + 1)
                db.collection(PRODUCTS_COLLECTION).document(productId)
                    .update("rating", averageRating).await()
            } else {
                db.collection(PRODUCTS_COLLECTION).document(productId)
                    .update("rating", newRating).await()
            }
        } catch (e: Exception) {
            // Игнорируем ошибку обновления рейтинга
        }
    }

    // Получить информацию о пользователе (старая версия - оставляем для совместимости)
    suspend fun getUserInfo(): UserInfo? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val document = db.collection(USERS_COLLECTION).document(userId).get().await()

            if (document.exists()) {
                UserInfo(
                    id = userId,
                    name = document.getString("name") ?: "",
                    email = document.getString("email") ?: "",
                    phone = document.getString("phone") ?: "",
                    address = document.getString("address") ?: "",
                    createdAt = document.getString("createdAt") ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Обновить информацию о пользователе (старая версия)
    suspend fun updateUserInfo(name: String, phone: String, address: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false

            val userData = hashMapOf(
                "name" to name,
                "phone" to phone,
                "address" to address
            )

            db.collection(USERS_COLLECTION).document(userId)
                .update(userData as Map<String, Any>).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Поиск товаров
    suspend fun searchProducts(query: String): List<Product> {
        return try {
            val snapshot = db.collection(PRODUCTS_COLLECTION).get().await()
            snapshot.documents.mapNotNull { document ->
                try {
                    val characteristicsMap = document.get("characteristics") as? Map<*, *>
                    val characteristics = mutableMapOf<String, String>()

                    characteristicsMap?.forEach { (key, value) ->
                        if (key is String && value is String) {
                            characteristics[key] = value
                        }
                    }

                    val product = Product(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        description = document.getString("description") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        imageUrl = document.getString("imageUrl") ?: "",
                        category = document.getString("category") ?: "",
                        rating = document.getDouble("rating") ?: 0.0,
                        available = document.getBoolean("available") ?: false,
                        characteristics = characteristics
                    )

                    if (product.name.contains(query, ignoreCase = true) ||
                        product.description.contains(query, ignoreCase = true) ||
                        product.category.contains(query, ignoreCase = true)) {
                        product
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Получить статистику пользователя
    suspend fun getUserStats(): UserStats {
        return try {
            val userId = auth.currentUser?.uid ?: return UserStats()

            val ordersQuery = db.collection(ORDERS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get().await()

            val orders = ordersQuery.documents.mapNotNull { document ->
                try {
                    Order(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        productId = document.getString("productId") ?: "",
                        productName = document.getString("productName") ?: "",
                        days = document.getLong("days")?.toInt() ?: 0,
                        totalPrice = document.getDouble("totalPrice") ?: 0.0,
                        status = document.getString("status") ?: "pending",
                        createdAt = document.getString("createdAt") ?: "",
                        startDate = document.getString("startDate") ?: "",
                        endDate = document.getString("endDate") ?: "",
                        userPhone = document.getString("userPhone") ?: "",
                        userAddress = document.getString("userAddress") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val totalOrders = orders.size
            val totalSpent = orders.sumOf { it.totalPrice }
            val activeOrders = orders.count { it.status == "confirmed" || it.status == "in_progress" }
            val completedOrders = orders.count { it.status == "completed" }

            // Обновляем статистику в профиле пользователя
            updateUserStatsInProfile(userId, totalOrders, totalSpent)

            UserStats(
                totalOrders = totalOrders,
                totalSpent = totalSpent,
                activeOrders = activeOrders,
                completedOrders = completedOrders
            )
        } catch (e: Exception) {
            UserStats()
        }
    }

    // Обновить статистику в профиле пользователя
    private suspend fun updateUserStatsInProfile(userId: String, totalOrders: Int, totalSpent: Double) {
        try {
            db.collection(USERS_COLLECTION).document(userId)
                .update(
                    mapOf(
                        "totalOrders" to totalOrders,
                        "totalSpent" to totalSpent
                    )
                ).await()
        } catch (e: Exception) {
            // Игнорируем ошибку обновления статистики
        }
    }

    // Обновить рейтинг пользователя
    suspend fun updateUserRating(userId: String, newRating: Double) {
        try {
            db.collection(USERS_COLLECTION).document(userId)
                .update("rating", newRating).await()
        } catch (e: Exception) {
            // Игнорируем ошибку обновления рейтинга
        }
    }

    // Получить все заказы пользователя (для админа)
    suspend fun getAllUserOrders(userId: String): List<Order> {
        return try {
            val querySnapshot = db.collection(ORDERS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get().await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    Order(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        productId = document.getString("productId") ?: "",
                        productName = document.getString("productName") ?: "",
                        days = document.getLong("days")?.toInt() ?: 0,
                        totalPrice = document.getDouble("totalPrice") ?: 0.0,
                        status = document.getString("status") ?: "pending",
                        createdAt = document.getString("createdAt") ?: "",
                        startDate = document.getString("startDate") ?: "",
                        endDate = document.getString("endDate") ?: "",
                        userPhone = document.getString("userPhone") ?: "",
                        userAddress = document.getString("userAddress") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Удалить пользователя
    suspend fun deleteUserAccount(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false

            // Удаляем данные пользователя из Firestore
            db.collection(USERS_COLLECTION).document(userId).delete().await()

            // Удаляем пользователя из Firebase Auth
            auth.currentUser?.delete()?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Проверить, существует ли пользователь
    suspend fun userExists(userId: String): Boolean {
        return try {
            val document = db.collection(USERS_COLLECTION).document(userId).get().await()
            document.exists()
        } catch (e: Exception) {
            false
        }
    }

    // Получить пользователя по ID
    suspend fun getUserById(userId: String): UserProfile? {
        return try {
            val document = db.collection(USERS_COLLECTION).document(userId).get().await()
            if (document.exists()) {
                val totalOrders = document.getLong("totalOrders")?.toInt() ?: 0
                val totalSpent = document.getDouble("totalSpent") ?: 0.0
                val rating = document.getDouble("rating") ?: 0.0

                UserProfile(
                    id = userId,
                    email = document.getString("email") ?: "",
                    name = document.getString("name") ?: "Пользователь",
                    phone = document.getString("phone") ?: "Не указан",
                    address = document.getString("address") ?: "Не указан",
                    createdAt = document.getString("createdAt") ?: "",
                    avatarUrl = null,
                    totalOrders = totalOrders,
                    totalSpent = totalSpent,
                    rating = rating
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ==================== МЕТОДЫ ДЛЯ КОРЗИНЫ ====================

    // Добавить товар в корзину
    suspend fun addToCart(productId: String, days: Int = 1): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false

            val cartItem = hashMapOf(
                "userId" to userId,
                "productId" to productId,
                "days" to days,
                "addedAt" to Date().toString(),
                "status" to "active"
            )

            db.collection(CART_COLLECTION).add(cartItem).await()
            true
        } catch (e: Exception) {
            println("❌ Ошибка при добавлении в корзину: ${e.message}")
            false
        }
    }

    // Удалить товар из корзины по ID корзины
    suspend fun removeCartItem(cartItemId: String): Boolean {
        return try {
            db.collection(CART_COLLECTION).document(cartItemId).delete().await()
            true
        } catch (e: Exception) {
            println("❌ Ошибка при удалении из корзины: ${e.message}")
            false
        }
    }

    // Обновить количество дней аренды
    suspend fun updateCartItemDays(cartItemId: String, days: Int): Boolean {
        return try {
            db.collection(CART_COLLECTION).document(cartItemId)
                .update("days", days).await()
            true
        } catch (e: Exception) {
            println("❌ Ошибка при обновлении срока аренды: ${e.message}")
            false
        }
    }

    // Получить все cart items пользователя
    suspend fun getCartItems(): List<CartItem> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            val querySnapshot = db.collection(CART_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .get().await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    CartItem(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        productId = document.getString("productId") ?: "",
                        days = document.getLong("days")?.toInt() ?: 1,
                        addedAt = document.getString("addedAt") ?: "",
                        status = document.getString("status") ?: "active"
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            println("❌ Ошибка при получении cart items: ${e.message}")
            emptyList()
        }
    }

    // Получить товары корзины с информацией о продуктах
    suspend fun getCartProducts(): List<CartProduct> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            // Получаем все cart items
            val cartItems = getCartItems()
            if (cartItems.isEmpty()) return emptyList()

            // Получаем информацию о каждом продукте
            val cartProducts = mutableListOf<CartProduct>()

            // Группируем одинаковые товары
            val groupedItems = cartItems.groupBy { "${it.productId}_${it.days}" }

            for ((_, items) in groupedItems) {
                if (items.isNotEmpty()) {
                    val firstItem = items.first()
                    val product = getProductById(firstItem.productId)
                    if (product != null) {
                        cartProducts.add(CartProduct(
                            cartItemId = firstItem.id,
                            product = product,
                            days = firstItem.days,
                            quantity = items.size
                        ))
                    }
                }
            }

            cartProducts
        } catch (e: Exception) {
            println("❌ Ошибка при получении товаров корзины: ${e.message}")
            emptyList()
        }
    }

    data class AdminStats(
        val totalUsers: Int = 0,
        val totalProducts: Int = 0,
        val totalOrders: Int = 0,
        val completedOrders: Int = 0,
        val pendingOrders: Int = 0,
        val totalRevenue: Double = 0.0
    )



    // Поток для отслеживания изменений в корзине
    fun getCartStream(): Flow<List<CartItem>> = flow {
        try {
            val userId = auth.currentUser?.uid ?: return@flow emit(emptyList())

            val querySnapshot = db.collection(CART_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .get().await()

            val cartItems = querySnapshot.documents.mapNotNull { document ->
                try {
                    CartItem(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        productId = document.getString("productId") ?: "",
                        days = document.getLong("days")?.toInt() ?: 1,
                        addedAt = document.getString("addedAt") ?: "",
                        status = document.getString("status") ?: "active"
                    )
                } catch (e: Exception) {
                    null
                }
            }
            emit(cartItems)
        } catch (e: Exception) {
            println("❌ Ошибка в потоке корзины: ${e.message}")
            emit(emptyList())
        }
    }

    // Очистить корзину
    suspend fun clearCart(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false

            val querySnapshot = db.collection(CART_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .get().await()

            querySnapshot.documents.forEach { document ->
                try {
                    document.reference.delete().await()
                } catch (e: Exception) {
                    println("❌ Ошибка при удалении элемента корзины: ${e.message}")
                }
            }
            true
        } catch (e: Exception) {
            println("❌ Ошибка при очистке корзины: ${e.message}")
            false
        }
    }



    // Создать заказ из корзины
    suspend fun createOrderFromCart(
        cartProducts: List<CartProduct>,
        userPhone: String = "",
        userAddress: String = ""
    ): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false

            for (cartProduct in cartProducts) {
                val startDate = Date().toString()
                val endDate = Date(System.currentTimeMillis() + cartProduct.days * 24 * 60 * 60 * 1000).toString()

                createOrder(
                    productId = cartProduct.product.id,
                    productName = cartProduct.product.name,
                    days = cartProduct.days,
                    totalPrice = cartProduct.product.price * cartProduct.days * cartProduct.quantity,
                    startDate = startDate,
                    endDate = endDate,
                    userPhone = userPhone,
                    userAddress = userAddress
                )
            }

            // Очищаем корзину после создания заказа
            clearCart()
            true
        } catch (e: Exception) {
            println("❌ Ошибка при создании заказа из корзины: ${e.message}")
            false
        }
    }

    // Устаревший метод, заменен на logout()
    fun logoutUser() {
        auth.signOut()
    }
}