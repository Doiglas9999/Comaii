package com.comaii.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.comaii.app.ui.util.formatPrice
import com.comaii.shared.domain.model.Order
import com.comaii.shared.domain.model.OrderStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.parameter.parametersOf

data class CustomerOrdersScreen(
    val companyId: String,
    val customerId: String,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<CustomerOrdersScreenModel> { parametersOf(companyId, customerId) }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Meus Pedidos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = { navigator.pop() }) {
                            Text(
                                "← Voltar",
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
        ) { padding ->
            when {
                state.isLoading -> {
                    Box(
                        Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.orders.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("📦", style = MaterialTheme.typography.displayMedium)
                            Text(
                                "Nenhum pedido ainda",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Seus pedidos aparecerão aqui.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.orders, key = { it.id }) { order ->
                            CustomerOrderCard(order = order)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerOrderCard(order: Order) {
    val statusColor = order.status.displayColor()
    val statusLabel = order.status.displayLabel()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header row: status pill + date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Status pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }

                if (order.createdAt > 0L) {
                    Text(
                        text = formatOrderDate(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Items list
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${item.quantity}x ${item.productName}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "R$ ${item.subtotal.formatPrice()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (order.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Obs: ${order.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "R$ ${order.total.formatPrice()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Status description
            val statusDescription = order.status.customerMessage()
            if (statusDescription.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
            }
        }
    }
}

private fun formatOrderDate(epochMillis: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = dt.dayOfMonth.toString().padStart(2, '0')
        val month = dt.monthNumber.toString().padStart(2, '0')
        val hour = dt.hour.toString().padStart(2, '0')
        val min = dt.minute.toString().padStart(2, '0')
        "$day/$month ${hour}h${min}"
    } catch (_: Exception) {
        ""
    }
}

private fun OrderStatus.displayLabel(): String = when (this) {
    OrderStatus.PENDING -> "Aguardando"
    OrderStatus.CONFIRMED -> "Confirmado"
    OrderStatus.PREPARING -> "Preparando"
    OrderStatus.READY -> "Pronto"
    OrderStatus.DELIVERING -> "Em entrega"
    OrderStatus.DELIVERED -> "Entregue ✓"
    OrderStatus.CANCELLED -> "Cancelado"
}

private fun OrderStatus.displayColor(): Color = when (this) {
    OrderStatus.PENDING -> Color(0xFFFF9800)
    OrderStatus.CONFIRMED -> Color(0xFF2196F3)
    OrderStatus.PREPARING -> Color(0xFF9C27B0)
    OrderStatus.READY -> Color(0xFF4CAF50)
    OrderStatus.DELIVERING -> Color(0xFF00BCD4)
    OrderStatus.DELIVERED -> Color(0xFF607D8B)
    OrderStatus.CANCELLED -> Color(0xFFF44336)
}

private fun OrderStatus.customerMessage(): String = when (this) {
    OrderStatus.PENDING -> "Aguardando a loja confirmar seu pedido..."
    OrderStatus.CONFIRMED -> "Pedido confirmado! Em breve será preparado."
    OrderStatus.PREPARING -> "Estamos preparando seu pedido 🍳"
    OrderStatus.READY -> "Pedido pronto! Aguarde a entrega."
    OrderStatus.DELIVERING -> "Pedido a caminho! 🚴"
    OrderStatus.DELIVERED -> "Pedido entregue. Obrigado!"
    OrderStatus.CANCELLED -> "Este pedido foi cancelado."
}
