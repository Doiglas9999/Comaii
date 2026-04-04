package com.comaii.shared.data.firebase

import com.comaii.shared.domain.model.Category
import com.comaii.shared.domain.model.Company
import com.comaii.shared.domain.model.Order
import com.comaii.shared.domain.model.OrderStatus
import com.comaii.shared.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Implementação Web (WasmJS) do FirebaseService.
 *
 * Usa interop JS com o Firebase SDK carregado via index.html.
 * Por enquanto, implementação stub que será conectada via JS interop.
 *
 * TODO: Conectar com Firebase JS SDK via external declarations
 */
class WasmFirebaseService : FirebaseService {
    private val _authState = MutableStateFlow<String?>(null)

    // Auth - TODO: JS interop
    override val currentUserId: String?
        get() = _authState.value

    override val isLoggedIn: Boolean
        get() = _authState.value != null

    override fun observeAuthState(): Flow<String?> = _authState

    override suspend fun signInWithEmail(email: String, password: String): Result<String> {
        return try {
            val userId = jsSignIn(email, password)
            _authState.value = userId
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<String> {
        return try {
            val userId = jsSignUp(email, password)
            _authState.value = userId
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        jsSignOut()
        _authState.value = null
    }

    // Companies
    override suspend fun getCompany(id: String): Company? = null
    override suspend fun getCompanyBySlug(slug: String): Company? = null
    override suspend fun saveCompany(company: Company): String = company.id
    override fun observeCompany(id: String): Flow<Company?> = flowOf(null)

    // Products
    override fun observeProducts(companyId: String): Flow<List<Product>> = flowOf(emptyList())
    override suspend fun saveProduct(product: Product): String = product.id
    override suspend fun deleteProduct(productId: String) {}

    // Categories
    override fun observeCategories(companyId: String): Flow<List<Category>> = flowOf(emptyList())
    override suspend fun saveCategory(category: Category): String = category.id
    override suspend fun deleteCategory(categoryId: String) {}

    // Orders
    override suspend fun createOrder(order: Order): String = order.id
    override fun observeOrders(companyId: String): Flow<List<Order>> = flowOf(emptyList())
    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus) {}
}

// JS interop stubs - serão implementados com o Firebase JS SDK
private fun jsSignIn(email: String, password: String): String = "web-user-stub"
private fun jsSignUp(email: String, password: String): String = "web-user-stub"
private fun jsSignOut() {}
