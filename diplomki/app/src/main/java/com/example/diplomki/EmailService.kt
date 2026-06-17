package com.example.diplomki.services

import com.example.diplomki.Product
import com.example.diplomki.UserProfile
import com.example.diplomki.bd.CartProduct
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// Модель для одного товара (для старой функции)
data class EmailRequest(
    val service_id: String,
    val template_id: String,
    val user_id: String,
    val accessToken: String,
    val template_params: TemplateParams
)

// Модель для корзины (несколько товаров)
data class CartEmailRequest(
    val service_id: String,
    val template_id: String,
    val user_id: String,
    val accessToken: String,
    val template_params: CartTemplateParams
)

// Параметры для одного товара
data class TemplateParams(
    val user_name: String,
    val user_phone: String,
    val user_email: String,
    val user_address: String,
    val product_name: String,
    val product_price: String,
    val rental_days: String,
    val total_price: String,
    val start_date: String,
    val end_date: String,
    val current_time: String
)

// Параметры для корзины (несколько товаров)
data class CartTemplateParams(
    val user_name: String,
    val user_phone: String,
    val user_email: String,
    val user_address: String,
    val products_list: String, // HTML список товаров
    val total_items: String,
    val total_days: String,
    val total_price: String,
    val start_date: String,
    val end_date: String,
    val current_time: String
)

// Более гибкая модель ответа
data class EmailResponse(
    val status: String? = null,
    val message: String? = null,
    val error: String? = null
)

interface EmailApi {
    @POST("api/v1.0/email/send")
    @Headers("Content-Type: application/json")
    suspend fun sendEmail(@Body request: EmailRequest): retrofit2.Response<EmailResponse>

    @POST("api/v1.0/email/send")
    @Headers("Content-Type: application/json")
    suspend fun sendCartEmail(@Body request: CartEmailRequest): retrofit2.Response<EmailResponse>
}

object EmailService {
    private const val BASE_URL = "https://api.emailjs.com/"

    // Создаем GSON с lenient режимом
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val api = retrofit.create(EmailApi::class.java)

    // Ваши данные с EmailJS
    private const val SERVICE_ID = "service_zu48i4t"
    private const val TEMPLATE_ID = "template_bvwqkil" // Для одного товара
    private const val CART_TEMPLATE_ID = "template_1z4sxqu" // Для корзины (можно создать отдельный)
    private const val PUBLIC_KEY = "5N73CORGSh7UKRBND"
    private const val PRIVATE_KEY = "3d1EGeXhq9ZN2GhkpPJ91"

    // Функция для отправки одного товара (старая, для совместимости)
    suspend fun sendOrderNotification(
        user: UserProfile,
        product: Product,
        days: Int,
        startDate: String,
        endDate: String,
        currentTime: String
    ): Result<Boolean> {
        return try {
            val totalPrice = product.price * days

            println("📧 Отправка email для одного товара...")
            println("📧 SERVICE_ID: $SERVICE_ID")
            println("📧 TEMPLATE_ID: $TEMPLATE_ID")
            println("📧 PUBLIC_KEY: $PUBLIC_KEY")
            println("📧 User: ${user.name}, ${user.email}")
            println("📧 Product: ${product.name}")

            val request = EmailRequest(
                service_id = SERVICE_ID,
                template_id = TEMPLATE_ID,
                user_id = PUBLIC_KEY,
                accessToken = PRIVATE_KEY,
                template_params = TemplateParams(
                    user_name = user.name,
                    user_phone = user.phone,
                    user_email = user.email,
                    user_address = user.address,
                    product_name = product.name,
                    product_price = "${product.price} ₽",
                    rental_days = days.toString(),
                    total_price = "${totalPrice} ₽",
                    start_date = startDate,
                    end_date = endDate,
                    current_time = currentTime
                )
            )

            println("📧 Request body: ${gson.toJson(request)}")
            val response = api.sendEmail(request)

            println("📧 Response code: ${response.code()}")

            if (response.isSuccessful) {
                println("✅ Email успешно отправлен!")
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                println("❌ Ошибка отправки: $errorBody")
                Result.failure(Exception("Ошибка отправки: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ Исключение: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Новая функция для отправки корзины (одно письмо со всеми товарами)
    suspend fun sendCartOrderNotification(
        user: UserProfile,
        cartProducts: List<CartProduct>,
        startDate: String,
        endDate: String,
        currentTime: String
    ): Result<Boolean> {
        return try {
            val totalItems = cartProducts.sumOf { it.quantity }
            val maxDays = cartProducts.maxOfOrNull { it.days } ?: 1
            val totalPrice = cartProducts.sumOf { it.product.price * it.days * it.quantity }

            // Формируем HTML список товаров
            val productsList = buildProductsListHtml(cartProducts)

            println("📧 Отправка email с корзиной (${cartProducts.size} товаров)...")
            println("📧 SERVICE_ID: $SERVICE_ID")
            println("📧 TEMPLATE_ID: $CART_TEMPLATE_ID")
            println("📧 PUBLIC_KEY: $PUBLIC_KEY")
            println("📧 User: ${user.name}, ${user.email}")
            println("📧 Товаров в корзине: ${cartProducts.size}")

            val request = CartEmailRequest(
                service_id = SERVICE_ID,
                template_id = CART_TEMPLATE_ID,
                user_id = PUBLIC_KEY,
                accessToken = PRIVATE_KEY,
                template_params = CartTemplateParams(
                    user_name = user.name,
                    user_phone = user.phone,
                    user_email = user.email,
                    user_address = user.address,
                    products_list = productsList,
                    total_items = totalItems.toString(),
                    total_days = maxDays.toString(),
                    total_price = "${totalPrice.toInt()} ₽",
                    start_date = startDate,
                    end_date = endDate,
                    current_time = currentTime
                )
            )

            println("📧 Request body: ${gson.toJson(request)}")
            val response = api.sendCartEmail(request)

            println("📧 Response code: ${response.code()}")

            if (response.isSuccessful) {
                println("✅ Email с корзиной успешно отправлен!")
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                println("❌ Ошибка отправки: $errorBody")
                Result.failure(Exception("Ошибка отправки: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ Исключение: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Вспомогательная функция для создания HTML списка товаров
    private fun buildProductsListHtml(cartProducts: List<CartProduct>): String {
        val sb = StringBuilder()
        sb.append("<table style='width: 100%; border-collapse: collapse; margin-top: 10px;'>")
        sb.append("<tr style='background-color: #f0f0f0;'>")
        sb.append("<th style='padding: 8px; text-align: left; border-bottom: 2px solid #ddd;'>Товар</th>")
        sb.append("<th style='padding: 8px; text-align: center; border-bottom: 2px solid #ddd;'>Цена/день</th>")
        sb.append("<th style='padding: 8px; text-align: center; border-bottom: 2px solid #ddd;'>Срок</th>")
        sb.append("<th style='padding: 8px; text-align: center; border-bottom: 2px solid #ddd;'>Кол-во</th>")
        sb.append("<th style='padding: 8px; text-align: right; border-bottom: 2px solid #ddd;'>Сумма</th>")
        sb.append("</tr>")

        cartProducts.forEachIndexed { index, item ->
            val itemTotal = item.product.price * item.days * item.quantity
            val backgroundColor = if (index % 2 == 0) "#ffffff" else "#f9f9f9"

            sb.append("<tr style='background-color: $backgroundColor;'>")
            sb.append("<td style='padding: 8px; border-bottom: 1px solid #eee;'>${item.product.name}</td>")
            sb.append("<td style='padding: 8px; border-bottom: 1px solid #eee; text-align: center;'>${item.product.price.toInt()} ₽</td>")
            sb.append("<td style='padding: 8px; border-bottom: 1px solid #eee; text-align: center;'>${item.days} дн.</td>")
            sb.append("<td style='padding: 8px; border-bottom: 1px solid #eee; text-align: center;'>${item.quantity}</td>")
            sb.append("<td style='padding: 8px; border-bottom: 1px solid #eee; text-align: right; font-weight: bold;'>${itemTotal.toInt()} ₽</td>")
            sb.append("</tr>")
        }

        sb.append("</table>")
        return sb.toString()
    }
}