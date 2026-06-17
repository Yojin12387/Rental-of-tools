package com.example.diplomki

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object TestData {

    suspend fun initializeProductsIfNeeded(): Boolean {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("products").get().await()
        if (snapshot.isEmpty) {
            println("📦 Коллекция 'products' пуста. Добавляем тестовые товары...")
            return addTestProducts()
        } else {
            println("📦 Товары уже существуют (${snapshot.size()} шт.). Пропускаем инициализацию.")
            return false
        }
    }

    private suspend fun addTestProducts(): Boolean {
        val db = FirebaseFirestore.getInstance()

        val testProducts = listOf(
            mapOf(
                "id" to "1",
                "name" to "Дрель Bosch Professional GSB 13 RE",
                "description" to "Мощная дрель с регулировкой скорости, идеальна для строительных работ. Максимальная мощность 850W, скорость вращения 0-3000 об/мин. Вес 1.8 кг, комплектуется набором сверл.",
                "price" to 800.0,
                "rating" to 4.7,
                "category" to "Электроинструменты",
                "available" to true,
                "imageUrl" to "https://firebasestorage.googleapis.com/v0/b/diplomki-xxxxx.appspot.com/o/products%2Fdrill.jpg?alt=media",
                "characteristics" to mapOf(
                    "Мощность" to "850 Вт",
                    "Скорость" to "0–3000 об/мин",
                    "Вес" to "1.8 кг",
                    "Патрон" to "Быстрозажимной",
                    "Гарантия" to "2 года"
                )
            ),
            mapOf(
                "id" to "2",
                "name" to "Перфоратор Makita HR2470",
                "description" to "Перфоратор с ударным механизмом для работы с бетоном и кирпичом. Мощность 780W, энергия удара 2.7 Дж. Режимы: сверление, ударное сверление, долбление.",
                "price" to 1200.0,
                "rating" to 4.9,
                "category" to "Электроинструменты",
                "available" to true,
                "imageUrl" to "https://firebasestorage.googleapis.com/v0/b/diplomki-xxxxx.appspot.com/o/products%2Fperforator.jpg?alt=media",
                "characteristics" to mapOf(
                    "Мощность" to "780 Вт",
                    "Энергия удара" to "2.7 Дж",
                    "Режимы" to "3 (сверление, удар, долбление)",
                    "Вес" to "3.2 кг",
                    "Комплектация" to "Кейс, 2 бура"
                )
            ),
            mapOf(
                "id" to "3",
                "name" to "Шуруповёрт DeWalt DCD771C2",
                "description" to "Аккумуляторный шуруповёрт с набором бит и быстрой зарядкой. Максимальный крутящий момент 50 Нм.",
                "price" to 600.0,
                "rating" to 4.5,
                "category" to "Электроинструменты",
                "available" to false,
                "imageUrl" to "https://firebasestorage.googleapis.com/v0/b/diplomki-xxxxx.appspot.com/o/products%2Fscrewdriver.jpg?alt=media",
                "characteristics" to mapOf(
                    "Тип" to "Аккумуляторный",
                    "Напряжение" to "18 В",
                    "Момент" to "50 Н·м",
                    "Аккумуляторы" to "2 × 2.0 А·ч",
                    "Время зарядки" to "60 мин"
                )
            ),
            mapOf(
                "id" to "4",
                "name" to "Набор гаечных ключей Kraftool 24 пр.",
                "description" to "Набор из 24 гаечных ключей разных размеров от 6 до 32 мм. Хромированное покрытие.",
                "price" to 300.0,
                "rating" to 4.4,
                "category" to "Ручные инструменты",
                "available" to true,
                "imageUrl" to "https://firebasestorage.googleapis.com/v0/b/diplomki-xxxxx.appspot.com/o/products%2Fwrench_set.jpg?alt=media",
                "characteristics" to mapOf(
                    "Количество" to "24 шт.",
                    "Диапазон" to "6–32 мм",
                    "Материал" to "Хромированная сталь",
                    "Гарантия" to "5 лет",
                    "Упаковка" to "Пластиковый кейс"
                )
            )
        )

        return try {
            for (product in testProducts) {
                db.collection("products")
                    .document(product["id"] as String)
                    .set(product)
                    .await()
            }
            println("✅ Успешно добавлено ${testProducts.size} тестовых товаров.")
            true
        } catch (e: Exception) {
            println("❌ Ошибка при добавлении тестовых товаров: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}