package com.comaii.app.ui.screens.admin.products

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.domain.model.Category
import com.comaii.shared.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showAddCategoryDialog: Boolean = false,
    val editingProduct: Product? = null,
    val formName: String = "",
    val formDescription: String = "",
    val formPrice: String = "",
    val formCategoryId: String = "",
    val formImageUrl: String = "",
    val formCategoryName: String = "",
    val error: String? = null,
)

class ProductsScreenModel(
    private val companyId: String,
    private val firebase: FirebaseService,
) : ScreenModel {

    private val _state = MutableStateFlow(ProductsUiState())
    val state: StateFlow<ProductsUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            firebase.observeProducts(companyId).collect { products ->
                _state.value = _state.value.copy(products = products, isLoading = false)
            }
        }
        screenModelScope.launch {
            firebase.observeCategories(companyId).collect { categories ->
                _state.value = _state.value.copy(categories = categories)
            }
        }
    }

    fun showAddProductDialog(product: Product? = null) {
        _state.value = _state.value.copy(
            showAddDialog = true,
            editingProduct = product,
            formName = product?.name ?: "",
            formDescription = product?.description ?: "",
            formPrice = product?.price?.toString() ?: "",
            formImageUrl = product?.imageUrl ?: "",
            formCategoryId = product?.categoryId ?: "",
            error = null,
        )
    }

    fun hideAddProductDialog() {
        _state.value = _state.value.copy(
            showAddDialog = false, editingProduct = null,
            formName = "", formDescription = "", formPrice = "", formImageUrl = "", formCategoryId = "",
        )
    }

    fun showAddCategoryDialog() {
        _state.value = _state.value.copy(showAddCategoryDialog = true, formCategoryName = "")
    }

    fun hideAddCategoryDialog() {
        _state.value = _state.value.copy(showAddCategoryDialog = false, formCategoryName = "")
    }

    fun onFormNameChange(value: String) { _state.value = _state.value.copy(formName = value, error = null) }
    fun onFormDescriptionChange(value: String) { _state.value = _state.value.copy(formDescription = value) }
    fun onFormPriceChange(value: String) { _state.value = _state.value.copy(formPrice = value, error = null) }
    fun onFormImageUrlChange(value: String) { _state.value = _state.value.copy(formImageUrl = value) }
    fun onFormCategoryIdChange(value: String) { _state.value = _state.value.copy(formCategoryId = value) }
    fun onFormCategoryNameChange(value: String) { _state.value = _state.value.copy(formCategoryName = value) }

    fun saveProduct() {
        val s = _state.value
        if (s.formName.isBlank() || s.formPrice.isBlank()) {
            _state.value = s.copy(error = "Nome e preço são obrigatórios")
            return
        }
        val price = s.formPrice.replace(",", ".").toDoubleOrNull()
        if (price == null || price <= 0) {
            _state.value = s.copy(error = "Preço inválido")
            return
        }

        screenModelScope.launch {
            try {
                val product = if (s.editingProduct != null) {
                    s.editingProduct.copy(
                        name = s.formName, description = s.formDescription,
                        price = price, imageUrl = s.formImageUrl,
                        categoryId = s.formCategoryId,
                    )
                } else {
                    Product(
                        companyId = companyId, name = s.formName,
                        description = s.formDescription, price = price,
                        imageUrl = s.formImageUrl, categoryId = s.formCategoryId,
                        order = s.products.size,
                    )
                }
                firebase.saveProduct(product)
                hideAddProductDialog()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Erro ao salvar: ${e.message}")
            }
        }
    }

    fun saveCategory() {
        val name = _state.value.formCategoryName
        if (name.isBlank()) return

        screenModelScope.launch {
            firebase.saveCategory(
                Category(companyId = companyId, name = name, order = _state.value.categories.size)
            )
            hideAddCategoryDialog()
        }
    }

    fun deleteProduct(productId: String) {
        screenModelScope.launch { firebase.deleteProduct(productId) }
    }
}
