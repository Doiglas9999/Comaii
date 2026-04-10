package com.comaii.app.ui.screens.customer

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.domain.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerOrdersUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
)

class CustomerOrdersScreenModel(
    private val companyId: String,
    private val customerId: String,
    private val firebase: FirebaseService,
) : ScreenModel {

    private val _state = MutableStateFlow(CustomerOrdersUiState())
    val state: StateFlow<CustomerOrdersUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            firebase.observeCustomerOrders(companyId, customerId).collect { orders ->
                _state.value = CustomerOrdersUiState(orders = orders, isLoading = false)
            }
        }
    }
}
