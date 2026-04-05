package com.comaii.shared.data.firebase

import com.comaii.shared.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

class WasmFirebaseService(
    private val httpClient: HttpClient
) : FirebaseService {

    private val apiKey = "AIzaSyAvu7ubJbNTsy-nOjmPZJpvEWMr4hspMcM"
    private val projectId = "comaii"
    private val authUrl = "https://identitytoolkit.googleapis.com/v1"
    private val fsUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

    private var idToken: String? = null
    private val _authState = MutableStateFlow<String?>(null)
    override val currentUserId: String? get() = _authState.value
    override val isLoggedIn: Boolean get() = _authState.value != null
    override fun observeAuthState(): Flow<String?> = _authState

    // ========== AUTH ==========

    @Serializable
    private data class AuthRequest(
        val email: String,
        val password: String,
        val returnSecureToken: Boolean = true
    )

    @Serializable
    private data class AuthResponse(
        val localId: String = "",
        val idToken: String = "",
        val error: AuthError? = null
    )

    @Serializable
    private data class AuthError(val message: String = "")

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private suspend fun authRequest(endpoint: String, email: String, password: String): Result<String> {
        return try {
            val response = httpClient.post("$authUrl/$endpoint?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(AuthRequest.serializer(), AuthRequest(email, password)))
            }
            val body = json.decodeFromString(AuthResponse.serializer(), response.bodyAsText())
            if (body.error != null) {
                Result.failure(Exception(body.error.message))
            } else {
                idToken = body.idToken
                _authState.value = body.localId
                Result.success(body.localId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<String> =
        authRequest("accounts:signInWithPassword", email, password)

    override suspend fun signUpWithEmail(email: String, password: String): Result<String> =
        authRequest("accounts:signUp", email, password)

    override suspend fun signOut() {
        idToken = null
        _authState.value = null
    }

    // ========== FIRESTORE HELPERS ==========

    private fun toFirestoreValue(element: JsonElement): JsonElement {
        return when {
            element is JsonPrimitive && element.isString ->
                JsonObject(mapOf("stringValue" to element))
            element is JsonPrimitive && element.booleanOrNull != null ->
                JsonObject(mapOf("booleanValue" to element))
            element is JsonPrimitive && element.longOrNull != null ->
                JsonObject(mapOf("integerValue" to JsonPrimitive(element.content)))
            element is JsonPrimitive && element.doubleOrNull != null ->
                JsonObject(mapOf("doubleValue" to element))
            element is JsonObject ->
                JsonObject(mapOf("mapValue" to JsonObject(mapOf(
                    "fields" to JsonObject(element.entries.associate { (k, v) -> k to toFirestoreValue(v) })
                ))))
            element is JsonArray ->
                JsonObject(mapOf("arrayValue" to JsonObject(mapOf(
                    "values" to JsonArray(element.map { toFirestoreValue(it) })
                ))))
            else -> JsonObject(mapOf("nullValue" to JsonPrimitive("NULL_VALUE")))
        }
    }

    private fun fromFirestoreValue(element: JsonElement): JsonElement {
        val obj = element as? JsonObject ?: return JsonNull
        return when {
            obj.containsKey("stringValue") -> obj["stringValue"]!!
            obj.containsKey("booleanValue") -> obj["booleanValue"]!!
            obj.containsKey("integerValue") -> {
                val s = (obj["integerValue"] as? JsonPrimitive)?.content ?: "0"
                JsonPrimitive(s.toLongOrNull() ?: 0L)
            }
            obj.containsKey("doubleValue") -> obj["doubleValue"]!!
            obj.containsKey("nullValue") -> JsonNull
            obj.containsKey("mapValue") -> {
                val fields = obj["mapValue"]?.jsonObject?.get("fields")?.jsonObject ?: return JsonNull
                JsonObject(fields.mapValues { (_, v) -> fromFirestoreValue(v) })
            }
            obj.containsKey("arrayValue") -> {
                val values = obj["arrayValue"]?.jsonObject?.get("values")?.jsonArray
                    ?: return JsonArray(emptyList())
                JsonArray(values.map { fromFirestoreValue(it) })
            }
            else -> JsonNull
        }
    }

    private fun documentToMap(docJson: JsonObject): Map<String, JsonElement> {
        val fields = docJson["fields"]?.jsonObject ?: return emptyMap()
        return fields.mapValues { (_, v) -> fromFirestoreValue(v) }
    }

    private inline fun <reified T> documentToObject(docJson: JsonObject): T? {
        return try {
            val map = documentToMap(docJson)
            json.decodeFromJsonElement(JsonObject(map))
        } catch (e: Exception) {
            null
        }
    }

    private inline fun <reified T> objectToFields(obj: T): JsonObject {
        val encoded = json.encodeToJsonElement(obj) as? JsonObject ?: return JsonObject(emptyMap())
        val fields = encoded.entries.associate { (k, v) -> k to toFirestoreValue(v) }
        return JsonObject(mapOf("fields" to JsonObject(fields)))
    }

    private fun authorizedRequest(builder: HttpRequestBuilder) {
        val token = idToken
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        } else {
            builder.parameter("key", apiKey)
        }
    }

    private suspend fun getDocument(path: String): JsonObject? {
        return try {
            val response = httpClient.get("$fsUrl/$path") {
                authorizedRequest(this)
            }
            if (response.status.isSuccess()) {
                json.parseToJsonElement(response.bodyAsText()).jsonObject
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun listDocuments(collectionPath: String): List<JsonObject> {
        return try {
            val response = httpClient.get("$fsUrl/$collectionPath") {
                authorizedRequest(this)
            }
            val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["documents"]?.jsonArray?.mapNotNull { it as? JsonObject } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun setDocument(path: String, fields: JsonObject): Boolean {
        return try {
            val response = httpClient.patch("$fsUrl/$path") {
                authorizedRequest(this)
                contentType(ContentType.Application.Json)
                setBody(fields.toString())
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun deleteDocument(path: String): Boolean {
        return try {
            val response = httpClient.delete("$fsUrl/$path") {
                authorizedRequest(this)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    private fun <T> pollingFlow(intervalMs: Long = 3000L, fetch: suspend () -> T): Flow<T> = flow {
        while (true) {
            try {
                emit(fetch())
            } catch (_: Exception) {}
            delay(intervalMs)
        }
    }

    private fun generateId(): String =
        Clock.System.now().toEpochMilliseconds().toString(36) +
                (0..999999).random().toString(36)

    // ========== COMPANIES ==========

    override suspend fun getCompany(id: String): Company? {
        val doc = getDocument("companies/$id") ?: return null
        return documentToObject<Company>(doc)
    }

    override suspend fun getCompanyBySlug(slug: String): Company? = null // Requires Firestore query; not supported via simple REST GET

    override suspend fun saveCompany(company: Company): String {
        objectToFields(company).let { setDocument("companies/${company.id}", it) }
        return company.id
    }

    override fun observeCompany(id: String): Flow<Company?> = pollingFlow {
        getCompany(id)
    }

    // ========== PRODUCTS ==========

    override fun observeProducts(companyId: String): Flow<List<Product>> = pollingFlow {
        listDocuments("companies/$companyId/products")
            .mapNotNull { documentToObject<Product>(it) }
            .sortedBy { it.order }
    }

    override suspend fun saveProduct(product: Product): String {
        val id = product.id.ifEmpty { generateId() }
        val p = product.copy(id = id)
        setDocument("companies/${p.companyId}/products/$id", objectToFields(p))
        return id
    }

    override suspend fun deleteProduct(companyId: String, productId: String) {
        deleteDocument("companies/$companyId/products/$productId")
    }

    // ========== CATEGORIES ==========

    override fun observeCategories(companyId: String): Flow<List<Category>> = pollingFlow {
        listDocuments("companies/$companyId/categories")
            .mapNotNull { documentToObject<Category>(it) }
            .sortedBy { it.order }
    }

    override suspend fun saveCategory(companyId: String, category: Category): String {
        val id = category.id.ifEmpty { generateId() }
        val c = category.copy(id = id, companyId = companyId)
        setDocument("companies/$companyId/categories/$id", objectToFields(c))
        return id
    }

    override suspend fun deleteCategory(companyId: String, categoryId: String) {
        deleteDocument("companies/$companyId/categories/$categoryId")
    }

    // ========== ORDERS ==========

    override suspend fun createOrder(companyId: String, order: Order): String {
        val id = generateId()
        val o = order.copy(
            id = id,
            companyId = companyId,
            createdAt = if (order.createdAt == 0L) Clock.System.now().toEpochMilliseconds() else order.createdAt
        )
        setDocument("companies/$companyId/orders/$id", objectToFields(o))
        return id
    }

    override fun observeOrders(companyId: String): Flow<List<Order>> = pollingFlow {
        listDocuments("companies/$companyId/orders")
            .mapNotNull { documentToObject<Order>(it) }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun updateOrderStatus(companyId: String, orderId: String, status: OrderStatus) {
        val doc = getDocument("companies/$companyId/orders/$orderId") ?: return
        val order = documentToObject<Order>(doc) ?: return
        val updated = order.copy(status = status)
        setDocument("companies/$companyId/orders/$orderId", objectToFields(updated))
    }

    // ========== EXPENSES ==========

    override suspend fun addExpense(companyId: String, expense: Expense): String {
        val id = generateId()
        val e = expense.copy(
            id = id,
            companyId = companyId,
            date = if (expense.date == 0L) Clock.System.now().toEpochMilliseconds() else expense.date
        )
        setDocument("companies/$companyId/expenses/$id", objectToFields(e))
        return id
    }

    override fun observeExpenses(companyId: String): Flow<List<Expense>> = pollingFlow {
        listDocuments("companies/$companyId/expenses")
            .mapNotNull { documentToObject<Expense>(it) }
            .sortedByDescending { it.date }
    }

    override suspend fun deleteExpense(companyId: String, expenseId: String) {
        deleteDocument("companies/$companyId/expenses/$expenseId")
    }
}
