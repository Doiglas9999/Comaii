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
) {
    val productsByCategory: Map<String, List<Product>>
        get() {
            val available = products.filter { it.isAvailable }
            val categoryMap = categories.associateBy { it.id }
            return available.groupBy { product ->
                categoryMap[product.categoryId]?.name ?: "Outros"
            }
        }

    val cartTotal: Double
        get() = cartItems.sumOf { (product, qty) -> product.price * qty }
}

class StoreScreenModel(
    private val companyId: String,
    private val firebase: FirebaseService,
) : ScreenModel {

    private val _state = MutableStateFlow(StoreUiState())
    val state: StateFlow<StoreUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            firebase.observeCompany(companyId).collect { company ->
                _state.value = _state.value.copy(company = company, isLoading = false)
            }
        }
        screenModelScope.launch {
            firebase.observeProducts(companyId).collect { products ->
                _state.value = _state.value.copy(products = products)
            }
        }
        screenModelScope.launch {
            firebase.observeCategories(companyId).collect { categories ->
                _state.value = _state.value.copy(categories = categories)
            }
        }
    }

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
}
