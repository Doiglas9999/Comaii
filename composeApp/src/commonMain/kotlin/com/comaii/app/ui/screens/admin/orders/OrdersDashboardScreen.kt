package com.comaii.app.ui.screens.admin.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.comaii.app.ui.util.formatPrice
import com.comaii.shared.domain.model.ExpenseType
import com.comaii.shared.domain.model.Order
import com.comaii.shared.domain.model.OrderStatus
import com.comaii.shared.domain.model.Expense
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.parameter.parametersOf

data class OrdersDashboardScreen(val companyId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<OrdersDashboardScreenModel> { parametersOf(companyId) }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var selectedTab by remember { mutableStateOf(0) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pedidos & Vendas") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    navigationIcon = {
                        TextButton(onClick = { navigator.pop() }) {
                            Text("Voltar", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                )
            },
            floatingActionButton = {
                if (selectedTab == 1) {
                    FloatingActionButton(
                        onClick = screenModel::showAddExpense,
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            "+",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // Filter row
                FilterRow(
                    selectedDays = state.filterDays,
                    onFilterSelected = screenModel::setFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                // Summary cards
                SummaryCardsRow(
                    totalSales = state.totalSales,
                    totalProductCosts = state.totalProductCosts,
                    totalExpenses = state.totalExpenses,
                    profit = state.profit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                )

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pedidos") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Custos") },
                    )
                }

                // Tab content
                when (selectedTab) {
                    0 -> OrdersSection(
                        orders = state.filteredOrders,
                        onStatusChange = screenModel::updateOrderStatus,
                        modifier = Modifier.fillMaxSize(),
                    )
                    1 -> ExpensesSection(
                        expenses = if (state.filterDays == 0) state.expenses else {
                            val cutoff = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() -
                                state.filterDays * 24L * 60 * 60 * 1000
                            state.expenses.filter { it.date >= cutoff }
                        },
                        onDelete = screenModel::deleteExpense,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Add expense dialog
        if (state.showAddExpenseDialog) {
            AddExpenseDialog(
                state = state,
                onDescriptionChange = screenModel::onExpenseDescriptionChange,
                onAmountChange = screenModel::onExpenseAmountChange,
                onTypeChange = screenModel::onExpenseTypeChange,
                onSave = screenModel::saveExpense,
                onDismiss = screenModel::hideAddExpense,
            )
        }
    }
}

@Composable
private fun FilterRow(
    selectedDays: Int,
    onFilterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = listOf(7 to "7 dias", 30 to "30 dias", 90 to "90 dias", 0 to "Tudo")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (days, label) ->
            val selected = selectedDays == days
            if (selected) {
                Button(
                    onClick = { onFilterSelected(days) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                OutlinedButton(
                    onClick = { onFilterSelected(days) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SummaryCardsRow(
    totalSales: Double,
    totalProductCosts: Double,
    totalExpenses: Double,
    profit: Double,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryCard(
            title = "Vendas",
            value = "R$ ${totalSales.formatPrice()}",
            valueColor = Color(0xFF2E7D32),
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            title = "Custos Produtos",
            value = "R$ ${totalProductCosts.formatPrice()}",
            valueColor = Color(0xFFE65100),
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            title = "Outros Custos",
            value = "R$ ${totalExpenses.formatPrice()}",
            valueColor = Color(0xFFC62828),
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            title = "Lucro",
            value = "R$ ${profit.formatPrice()}",
            valueColor = if (profit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
        }
    }
}

@Composable
private fun OrdersSection(
    orders: List<Order>,
    onStatusChange: (String, OrderStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (orders.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Nenhum pedido no período",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        val sortedOrders = orders.sortedByDescending { it.createdAt }
        LazyColumn(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sortedOrders, key = { it.id }) { order ->
                OrderCard(order = order, onStatusChange = onStatusChange)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderCard(
    order: Order,
    onStatusChange: (String, OrderStatus) -> Unit,
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "R$ ${order.total.formatPrice()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatTimestamp(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Status chip with dropdown
                ExposedDropdownMenuBox(
                    expanded = statusMenuExpanded,
                    onExpandedChange = { statusMenuExpanded = it },
                ) {
                    Box(
                        modifier = Modifier
                            .menuAnchor()
                            .padding(4.dp),
                    ) {
                        StatusChip(status = order.status)
                    }
                    ExposedDropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false },
                    ) {
                        OrderStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.label()) },
                                onClick = {
                                    onStatusChange(order.id, status)
                                    statusMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Order items summary
            if (order.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = order.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val (bgColor, textColor) = statusColors(status)
    Box(
        modifier = Modifier
            .then(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
        ) {
            Text(
                text = status.label(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

private fun statusColors(status: OrderStatus): Pair<Color, Color> = when (status) {
    OrderStatus.PENDING -> Color(0xFFFFF9C4) to Color(0xFFF57F17)
    OrderStatus.CONFIRMED -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
    OrderStatus.PREPARING -> Color(0xFFFFF3E0) to Color(0xFFE65100)
    OrderStatus.READY -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
    OrderStatus.DELIVERING -> Color(0xFFE8EAF6) to Color(0xFF283593)
    OrderStatus.DELIVERED -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
    OrderStatus.CANCELLED -> Color(0xFFFFEBEE) to Color(0xFFB71C1C)
}

private fun OrderStatus.label(): String = when (this) {
    OrderStatus.PENDING -> "Pendente"
    OrderStatus.CONFIRMED -> "Confirmado"
    OrderStatus.PREPARING -> "Preparando"
    OrderStatus.READY -> "Pronto"
    OrderStatus.DELIVERING -> "Saiu p/ entrega"
    OrderStatus.DELIVERED -> "Entregue"
    OrderStatus.CANCELLED -> "Cancelado"
}

@Composable
private fun ExpensesSection(
    expenses: List<Expense>,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (expenses.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Nenhum custo registrado",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        val sortedExpenses = expenses.sortedByDescending { it.date }
        LazyColumn(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sortedExpenses, key = { it.id }) { expense ->
                ExpenseCard(expense = expense, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    onDelete: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = expense.type.label(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = formatTimestamp(expense.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "R$ ${expense.amount.formatPrice()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828),
            )
            TextButton(onClick = { onDelete(expense.id) }) {
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    state: OrdersDashboardUiState,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onTypeChange: (ExpenseType) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo custo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.expenseDescription,
                    onValueChange = onDescriptionChange,
                    label = { Text("Descrição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.expenseAmount,
                    onValueChange = onAmountChange,
                    label = { Text("Valor (R$)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it },
                ) {
                    OutlinedTextField(
                        value = state.expenseType.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                    ) {
                        ExpenseType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label()) },
                                onClick = {
                                    onTypeChange(type)
                                    typeExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private fun formatTimestamp(millis: Long): String {
    if (millis == 0L) return ""
    val instant = Instant.fromEpochMilliseconds(millis)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val day = local.dayOfMonth.toString().padStart(2, '0')
    val month = local.monthNumber.toString().padStart(2, '0')
    val year = local.year
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "$day/$month/$year $hour:$minute"
}
