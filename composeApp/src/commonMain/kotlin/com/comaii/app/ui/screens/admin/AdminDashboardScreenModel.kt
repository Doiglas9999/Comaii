package com.comaii.app.ui.screens.admin

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.domain.model.Company
import com.comaii.shared.domain.model.OrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val company: Company? = null,
    val productCount: Int = 0,
    val categoryCount: Int = 0,
    val orderCount: Int = 0,
    val pendingOrderCount: Int = 0,
    val isLoading: Boolean = true,
)

class AdminDashboardScreenModel(
    private val companyId: String,
    private val firebase: FirebaseService,
) : ScreenModel {

    private val _state = MutableStateFlow(AdminDashboardUiState())
    val state: StateFlow<AdminDashboardUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        screenModelScope.launch {
            firebase.observeCompany(companyId).collect { company ->
                _state.value = _state.value.copy(company = company)
            }
        }
        screenModelScope.launch {
            firebase.observeProducts(companyId).collect { products ->
                _state.value = _state.value.copy(productCount = products.size, isLoading = false)
            }
        }
        screenModelScope.launch {
            firebase.observeCategories(companyId).collect { categories ->
                _state.value = _state.value.copy(categoryCount = categories.size)
            }
        }
        screenModelScope.launch {
            firebase.observeOrders(companyId).collect { orders ->
                _state.value = _state.value.copy(
                    orderCount = orders.size,
                    pendingOrderCount = orders.count { it.status == OrderStatus.PENDING },
                )
            }
        }
    }
}
