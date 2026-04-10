package com.comaii.app.ui.screens.store

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.domain.model.Category
import com.comaii.shared.domain.model.Company
import com.comaii.shared.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoreUiState(
    val company: Company? = null,
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val cartItems: List<Pair<Product, Int>> = emptyList(),
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    // Auth state
    val isLoggedIn: Boolean = false,
    val currentUserId: String? = null,
    // Auth form fields
    val authEmail: String = "",
    val authPassword: String = "",
    val authName: String = "",
    val authPhone: String = "",
    val authIsLogin: Boolean = true,
    val authError: String? = null,
    val isAuthLoading: Boolean = false,
    // Category filter
    val selectedCategory: String? = null,
) {
    val productsByCategory: Map<String, List<Product>>
        get() {
            val available = products.filter { it.isAvailable }
            val categoryMap = categories.associateBy { it.id }
            return available.groupBy { product ->
                categoryMap[product.categoryId]?.name ?: "Outros"
            }
        }

    val filteredProducts: List<Product>
        get() {
            val available = products.filter { it.isAvailable }
            if (selectedCategory == null) return available
            val categoryMap = categories.associateBy { it.id }
            return available.filter { product ->
                (categoryMap[product.categoryId]?.name ?: "Outros") == selectedCategory
            }
        }

    val categoryNames: List<String>
        get() {
            val categoryMap = categories.associateBy { it.id }
            return products.filter { it.isAvailable }
                .map { categoryMap[it.categoryId]?.name ?: "Outros" }
                .distinct()
        }

    val cartTotal: Double
        get() = cartItems.sumOf { (product, qty) -> product.price * qty }

    val cartItemCount: Int
        get() = cartItems.sumOf { it.second }
}

class StoreScreenModel(
    // storeIdentifier pode ser slug ("haus") ou Firebase UID ("8L24nRz...")
    private val storeIdentifier: String,
    private val firebase: FirebaseService,
) : ScreenModel {

    private val _state = MutableStateFlow(StoreUiState())
    val state: StateFlow<StoreUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            resolveAndLoad()
        }
        screenModelScope.launch {
            firebase.observeAuthState().collect { userId ->
                _state.value = _state.value.copy(
                    isLoggedIn = userId != null,
                    currentUserId = userId,
                )
            }
        }
    }

    private suspend fun resolveAndLoad() {
        val company = firebase.getCompanyBySlug(storeIdentifier)
            ?: firebase.getCompany(storeIdentifier)

        if (company == null) {
            _state.value = _state.value.copy(isLoading = false, notFound = true)
            return
        }

        val resolvedId = company.id

        screenModelScope.launch {
            firebase.observeCompany(resolvedId).collect { updatedCompany ->
                _state.value = _state.value.copy(company = updatedCompany, isLoading = false)
            }
        }

        screenModelScope.launch {
            firebase.observeProducts(resolvedId).collect { products ->
                _state.value = _state.value.copy(products = products)
            }
        }

        screenModelScope.launch {
            firebase.observeCategories(resolvedId).collect { categories ->
                _state.value = _state.value.copy(categories = categories)
            }
        }
    }

    // ---- Auth actions ----

    fun onAuthEmailChange(v: String) = update { copy(authEmail = v, authError = null) }
    fun onAuthPasswordChange(v: String) = update { copy(authPassword = v, authError = null) }
    fun onAuthNameChange(v: String) = update { copy(authName = v) }
    fun onAuthPhoneChange(v: String) = update { copy(authPhone = v) }
    fun toggleAuthMode() = update { copy(authIsLogin = !authIsLogin, authError = null) }

    fun submitAuth() {
        val current = _state.value
        if (current.authEmail.isBlank() || current.authPassword.isBlank()) return
        screenModelScope.launch {
            _state.value = _state.value.copy(isAuthLoading = true, authError = null)
            val result = if (current.authIsLogin) {
                firebase.signInWithEmail(current.authEmail, current.authPassword)
            } else {
                firebase.signUpWithEmail(current.authEmail, current.authPassword)
            }
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isAuthLoading = false, authError = null)
                },
                onFailure = { err ->
                    val msg = when {
                        err.message?.contains("INVALID_PASSWORD") == true ||
                        err.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                            "E-mail ou senha incorretos"
                        err.message?.contains("EMAIL_EXISTS") == true ->
                            "Este e-mail já está cadastrado"
                        err.message?.contains("WEAK_PASSWORD") == true ->
                            "Senha muito fraca (mínimo 6 caracteres)"
                        err.message?.contains("INVALID_EMAIL") == true ->
                            "E-mail inválido"
                        else -> "Erro ao autenticar. Tente novamente."
                    }
                    _state.value = _state.value.copy(isAuthLoading = false, authError = msg)
                },
            )
        }
    }

    fun signOut() {
        screenModelScope.launch { firebase.signOut() }
    }

    // ---- Cart actions ----

    fun addToCart(product: Product) {
        val current = _state.value.cartItems.toMutableList()
        val index = current.indexOfFirst { it.first.id == product.id }
        if (index >= 0) {
            current[index] = current[index].copy(second = current[index].second + 1)
        } else {
            current.add(product to 1)
        }
        _state.value = _state.value.copy(cartItems = current)
    }

    fun removeFromCart(product: Product) {
        val current = _state.value.cartItems.toMutableList()
        val index = current.indexOfFirst { it.first.id == product.id }
        if (index >= 0) {
            val newQty = current[index].second - 1
            if (newQty <= 0) current.removeAt(index)
            else current[index] = current[index].copy(second = newQty)
        }
        _state.value = _state.value.copy(cartItems = current)
    }

    fun clearCart() {
        _state.value = _state.value.copy(cartItems = emptyList())
    }

    fun selectCategory(category: String?) {
        _state.value = _state.value.copy(selectedCategory = category)
    }

    private fun update(block: StoreUiState.() -> StoreUiState) {
        _state.value = _state.value.block()
    }
}
