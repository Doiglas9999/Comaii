package com.comaii.shared.data.firebase

import com.comaii.shared.domain.model.Category
import com.comaii.shared.domain.model.Company
import com.comaii.shared.domain.model.Expense
import com.comaii.shared.domain.model.Order
import com.comaii.shared.domain.model.OrderStatus
import com.comaii.shared.domain.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Interface multiplataforma para operações Firebase.
 * Cada plataforma (Android/Web) implementa de acordo com seu SDK.
 */
interface FirebaseService {
    // Auth
    val currentUserId: String?
    val isLoggedIn: Boolean
    fun observeAuthState(): Flow<String?> // emits userId or null
    suspend fun signInWithEmail(email: String, password: String): Result<String> // returns userId
    suspend fun signUpWithEmail(email: String, password: String): Result<String>
    suspend fun signOut()

    // Companies
    suspend fun getCompany(id: String): Company?
    suspend fun getCompanyBySlug(slug: String): Company?
    suspend fun saveCompany(company: Company): String
    fun observeCompany(id: String): Flow<Company?>

    // Products
    fun observeProducts(companyId: String): Flow<List<Product>>
    suspend fun saveProduct(product: Product): String
    suspend fun deleteProduct(companyId: String, productId: String)

    // Categories
    fun observeCategories(companyId: String): Flow<List<Category>>
    suspend fun saveCategory(companyId: String, category: Category): String
    suspend fun deleteCategory(companyId: String, categoryId: String)

    // Orders
    suspend fun createOrder(companyId: String, order: Order): String
    fun observeOrders(companyId: String): Flow<List<Order>>
    suspend fun updateOrderStatus(companyId: String, orderId: String, status: OrderStatus)
    fun observeCustomerOrders(companyId: String, customerId: String): Flow<List<Order>>

    // Expenses
    suspend fun addExpense(companyId: String, expense: Expense): String
    fun observeExpenses(companyId: String): Flow<List<Expense>>
    suspend fun deleteExpense(companyId: String, expenseId: String)
}
