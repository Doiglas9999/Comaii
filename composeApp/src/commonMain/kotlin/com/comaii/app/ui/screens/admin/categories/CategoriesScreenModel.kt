package com.comaii.app.ui.screens.admin.categories

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.domain.model.Category
import com.comaii.shared.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val categoryToDelete: Category? = null,
    val formName: String = "",
    val error: String? = null,
)

class CategoriesScreenModel(
    private val companyId: String,
    private val firebase: FirebaseService,
) : ScreenModel {
    private val _state = MutableStateFlow(CategoriesUiState())
    val state: StateFlow<CategoriesUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            firebase.observeCategories(companyId).collect { categories ->
                _state.value = _state.value.copy(categories = categories, isLoading = false)
            }
        }
        screenModelScope.launch {
            firebase.observeProducts(companyId).collect { products ->
                _state.value = _state.value.copy(products = products)
            }
        }
    }

    fun onFormNameChange(value: String) { _state.value = _state.value.copy(formName = value, error = null) }

    fun showAddDialog() { _state.value = _state.value.copy(showAddDialog = true, formName = "", error = null) }
    fun hideAddDialog() { _state.value = _state.value.copy(showAddDialog = false) }

    fun requestDelete(category: Category) {
        _state.value = _state.value.copy(showDeleteConfirm = true, categoryToDelete = category)
    }
    fun cancelDelete() { _state.value = _state.value.copy(showDeleteConfirm = false, categoryToDelete = null) }

    fun saveCategory() {
        val name = _state.value.formName.trim()
        if (name.isBlank()) { _state.value = _state.value.copy(error = "Informe um nome"); return }
        screenModelScope.launch {
            firebase.saveCategory(companyId, Category(companyId = companyId, name = name, order = _state.value.categories.size))
            hideAddDialog()
        }
    }

    fun confirmDelete() {
        val category = _state.value.categoryToDelete ?: return
        val productsInCategory = _state.value.products.filter { it.categoryId == category.id }
        screenModelScope.launch {
            // Move products to "Sem categoria" before deleting
            productsInCategory.forEach { product ->
                firebase.saveProduct(product.copy(categoryId = ""))
            }
            firebase.deleteCategory(companyId, category.id)
            _state.value = _state.value.copy(showDeleteConfirm = false, categoryToDelete = null)
        }
    }
}
