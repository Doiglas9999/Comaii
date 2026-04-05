package com.comaii.shared.data.firebase

import com.comaii.shared.domain.model.Category
import com.comaii.shared.domain.model.Company
import com.comaii.shared.domain.model.Expense
import com.comaii.shared.domain.model.Order
import com.comaii.shared.domain.model.OrderStatus
import com.comaii.shared.domain.model.Product
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class AndroidFirebaseService : FirebaseService {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    // ========== AUTH ==========
    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val isLoggedIn: Boolean
        get() = auth.currentUser != null

    override fun observeAuthState(): Flow<String?> {
        return auth.authStateChanged.map { it?.uid }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password)
            Result.success(result.user!!.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password)
            Result.success(result.user!!.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    // ========== COMPANIES ==========
    override suspend fun getCompany(id: String): Company? {
        return try {
            val doc = firestore.collection("companies").document(id).get()
            if (doc.exists) doc.data<Company>() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getCompanyBySlug(slug: String): Company? {
        return try {
            val query = firestore.collection("companies")
                .where { "slug" equalTo slug }.get()
            query.documents.firstOrNull()?.data<Company>()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveCompany(company: Company): String {
        val col = firestore.collection("companies")
        return if (company.id.isEmpty()) {
            val docRef = col.document
            val newCompany = company.copy(id = docRef.id)
            docRef.set(newCompany)
            docRef.id
        } else {
            col.document(company.id).set(company)
            company.id
        }
    }

    override fun observeCompany(id: String): Flow<Company?> {
        return firestore.collection("companies").document(id).snapshots.map { snapshot ->
            if (snapshot.exists) snapshot.data<Company>() else null
        }
    }

    // ========== PRODUCTS ==========
    override fun observeProducts(companyId: String): Flow<List<Product>> {
        return firestore.collection("companies").document(companyId).collection("products")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data<Product>() }.sortedBy { it.order }
            }
    }

    override suspend fun saveProduct(product: Product): String {
        val col = firestore.collection("companies").document(product.companyId).collection("products")
        return if (product.id.isEmpty()) {
            val docRef = col.document
            val newProduct = product.copy(id = docRef.id)
            docRef.set(newProduct)
            docRef.id
        } else {
            col.document(product.id).set(product)
            product.id
        }
    }

    override suspend fun deleteProduct(companyId: String, productId: String) {
        firestore.collection("companies").document(companyId).collection("products").document(productId).delete()
    }

    // ========== CATEGORIES ==========
    override fun observeCategories(companyId: String): Flow<List<Category>> {
        return firestore.collection("companies").document(companyId).collection("categories")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data<Category>() }.sortedBy { it.order }
            }
    }

    override suspend fun saveCategory(companyId: String, category: Category): String {
        val col = firestore.collection("companies").document(category.companyId).collection("categories")
        return if (category.id.isEmpty()) {
            val docRef = col.document
            val newCategory = category.copy(id = docRef.id)
            docRef.set(newCategory)
            docRef.id
        } else {
            col.document(category.id).set(category)
            category.id
        }
    }

    override suspend fun deleteCategory(companyId: String, categoryId: String) {
        firestore.collection("companies").document(companyId).collection("categories").document(categoryId).delete()
    }

    // ========== ORDERS ==========
    override suspend fun createOrder(companyId: String, order: Order): String {
        val col = firestore.collection("companies").document(order.companyId).collection("orders")
        val docRef = col.document
        val newOrder = order.copy(
            id = docRef.id,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        docRef.set(newOrder)
        return docRef.id
    }

    override fun observeOrders(companyId: String): Flow<List<Order>> {
        return firestore.collection("companies").document(companyId).collection("orders")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data<Order>() }
                    .sortedByDescending { it.createdAt }
            }
    }

    override suspend fun updateOrderStatus(companyId: String, orderId: String, status: OrderStatus) {
        firestore.collection("companies").document(companyId).collection("orders").document(orderId).update("status" to status.name)
    }

    // ========== EXPENSES ==========
    override suspend fun addExpense(companyId: String, expense: Expense): String {
        val col = firestore.collection("companies").document(companyId).collection("expenses")
        val docRef = col.document
        val newExpense = expense.copy(id = docRef.id, companyId = companyId, date = if (expense.date == 0L) Clock.System.now().toEpochMilliseconds() else expense.date)
        docRef.set(newExpense)
        return docRef.id
    }

    override fun observeExpenses(companyId: String): Flow<List<Expense>> {
        return firestore.collection("companies").document(companyId).collection("expenses")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data<Expense>() }.sortedByDescending { it.date }
            }
    }

    override suspend fun deleteExpense(companyId: String, expenseId: String) {
        firestore.collection("companies").document(companyId).collection("expenses").document(expenseId).delete()
    }
}
