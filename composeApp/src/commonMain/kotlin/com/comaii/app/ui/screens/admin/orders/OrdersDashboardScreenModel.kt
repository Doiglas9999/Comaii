package com.comaii.app.ui.screens.admin.orders

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.domain.model.Expense
import com.comaii.shared.domain.model.ExpenseType
import com.comaii.shared.domain.model.Order
import com.comaii.shared.domain.model.OrderStatus
import com.comaii.shared.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

data class OrdersDashboardUiState(
    val orders: List<Order> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val products: List<Product> = emptyList(),
    val filterDays: Int = 30,
    val showAddExpenseDialog: Boolean = false,
    val expenseDescription: String = "",
    val expenseAmount: String = "",
    val expenseType: ExpenseType = ExpenseType.OTHER,
    val isLoading: Boolean = true,
) {
    val filteredOrders: List<Order>
        get() {
            if (filterDays == 0) return orders
            val cutoff = nowMillis() - filterDays * 24L * 60 * 60 * 1000
            return orders.filter { it.createdAt >= cutoff }
        }

    val totalSales: Double
        get() = filteredOrders
            .filter { it.status == OrderStatus.DELIVERED || it.status == OrderStatus.CONFIRMED }
            .sumOf { it.total }

    val totalProductCosts: Double
        get() {
            val productMap = products.associateBy { it.id }
            return filteredOrders
                .filter { it.status == OrderStatus.DELIVERED || it.status == OrderStatus.CONFIRMED }
                .flatMap { it.items }
                .sumOf { item ->
                    val cost = productMap[item.productId]?.cost ?: 0.0
                    item.quantity * cost
                }
        }

    val totalExpenses: Double
        get() {
            if (filterDays == 0) return expenses.sumOf { it.amount }
            val cutoff = nowMillis() - filterDays * 24L * 60 * 60 * 1000
            return expenses.filter { it.date >= cutoff }.sumOf { it.amount }
        }

    val profit: Double
        get() = totalSales - totalProductCosts - totalExpenses
}

class OrdersDashboardScreenModel(
    private val companyId: String,
    private val firebase: FirebaseService,
) : ScreenModel {

    private val _state = MutableStateFlow(OrdersDashboardUiState())
    val state: StateFlow<OrdersDashboardUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        screenModelScope.launch {
            firebase.observeOrders(companyId).collect { orders ->
                _state.value = _state.value.copy(orders = orders, isLoading = false)
            }
        }
        screenModelScope.launch {
            firebase.observeExpenses(companyId).collect { expenses ->
                _state.value = _state.value.copy(expenses = expenses)
            }
        }
        screenModelScope.launch {
            firebase.observeProducts(companyId).collect { products ->
                _state.value = _state.value.copy(products = products)
            }
        }
    }

    fun setFilter(days: Int) {
        _state.value = _state.value.copy(filterDays = days)
    }

    fun showAddExpense() {
        _state.value = _state.value.copy(
            showAddExpenseDialog = true,
            expenseDescription = "",
            expenseAmount = "",
            expenseType = ExpenseType.OTHER,
        )
    }

    fun hideAddExpense() {
        _state.value = _state.value.copy(showAddExpenseDialog = false)
    }

    fun onExpenseDescriptionChange(value: String) {
        _state.value = _state.value.copy(expenseDescription = value)
    }

    fun onExpenseAmountChange(value: String) {
        _state.value = _state.value.copy(expenseAmount = value)
    }

    fun onExpenseTypeChange(value: ExpenseType) {
        _state.value = _state.value.copy(expenseType = value)
    }

    fun saveExpense() {
        val s = _state.value
        val amount = s.expenseAmount.replace(",", ".").toDoubleOrNull() ?: return
        if (amount <= 0 || s.expenseDescription.isBlank()) return

        val expense = Expense(
            companyId = companyId,
            description = s.expenseDescription,
            amount = amount,
            type = s.expenseType,
            date = nowMillis(),
        )
        screenModelScope.launch {
            firebase.addExpense(companyId, expense)
            hideAddExpense()
        }
    }

    fun deleteExpense(id: String) {
        screenModelScope.launch {
            firebase.deleteExpense(companyId, id)
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        screenModelScope.launch {
            firebase.updateOrderStatus(companyId, orderId, status)
        }
    }
}
