package com.comaii.app.ui.screens.admin.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val ACTIVE_STATUSES = setOf(
    OrderStatus.PENDING,
    OrderStatus.CONFIRMED,
    OrderStatus.PREPARING,
    OrderStatus.READY,
    OrderStatus.DELIVERING,
)

// Progressão natural de status
private fun OrderStatus.next(): OrderStatus? = when (this) {
    OrderStatus.PENDING    -> OrderStatus.CONFIRMED
    OrderStatus.CONFIRMED  -> OrderStatus.PREPARING
    OrderStatus.PREPARING  -> OrderStatus.READY
    OrderStatus.READY      -> OrderStatus.DELIVERING
    OrderStatus.DELIVERING -> OrderStatus.DELIVERED
    else                   -> null
}

private fun OrderStatus.nextLabel(): String = when (this) {
    OrderStatus.PENDING    -> "✓ Confirmar"
    OrderStatus.CONFIRMED  -> "▶ Iniciar preparo"
    OrderStatus.PREPARING  -> "✓ Marcar pronto"
    OrderStatus.READY      -> "🛵 Saiu p/ entrega"
    OrderStatus.DELIVERING -> "✓ Entregue"
    else                   -> ""
}

private fun OrderStatus.statusColor(): Color = when (this) {
    OrderStatus.PENDING    -> Color(0xFFF57F17)
    OrderStatus.CONFIRMED  -> Color(0xFF1565C0)
    OrderStatus.PREPARING  -> Color(0xFFE65100)
    OrderStatus.READY      -> Color(0xFF6A1B9A)
    OrderStatus.DELIVERING -> Color(0xFF283593)
    OrderStatus.DELIVERED  -> Color(0xFF2E7D32)
    OrderStatus.CANCELLED  -> Color(0xFFB71C1C)
}

private fun OrderStatus.label(): String = when (this) {
    OrderStatus.PENDING    -> "Pendente"
    OrderStatus.CONFIRMED  -> "Confirmado"
    OrderStatus.PREPARING  -> "Preparando"
    OrderStatus.READY      -> "Pronto"
    OrderStatus.DELIVERING -> "Saiu p/ entrega"
    OrderStatus.DELIVERED  -> "Entregue"
    OrderStatus.CANCELLED  -> "Cancelado"
}

private fun formatWhatsAppNumber(phone: String): String =
    phone.filter { it.isDigit() }.let {
        if (it.startsWith("55")) it else "55$it"
    }

private fun orderTimestamp(millis: Long): String {
    if (millis == 0L) return ""
    val instant = Instant.fromEpochMilliseconds(millis)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val day = local.dayOfMonth.toString().padStart(2, '0')
    val month = local.monthNumber.toString().padStart(2, '0')
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "$day/$month $hour:$minute"
}

// ─── Filtros de status ────────────────────────────────────────────────────────

private enum class OrderFilter(val label: String) {
    ALL("Todos"),
    PENDING("Pendentes"),
    PREPARING("Preparando"),
    READY("Pronto / Entrega"),
    DONE("Concluídos"),
}

private fun Order.matchesFilter(filter: OrderFilter): Boolean = when (filter) {
    OrderFilter.ALL       -> true
    OrderFilter.PENDING   -> status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED
    OrderFilter.PREPARING -> status == OrderStatus.PREPARING
    OrderFilter.READY     -> status == OrderStatus.READY || status == OrderStatus.DELIVERING
    OrderFilter.DONE      -> status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED
}

// ─── Screen ───────────────────────────────────────────────────────────────────

data class OrdersListScreen(val companyId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<OrdersDashboardScreenModel> { parametersOf(companyId) }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var selectedFilter by remember { mutableStateOf(OrderFilter.ALL) }

        val activeOrders = state.orders
            .filter { it.status in ACTIVE_STATUSES }
            .sortedBy { it.createdAt }

        val doneOrders = state.orders
            .filter { it.status !in ACTIVE_STATUSES }
            .sortedByDescending { it.createdAt }

        val allOrders = activeOrders + doneOrders
        val filteredOrders = allOrders.filter { it.matchesFilter(selectedFilter) }
        val pendingCount = state.orders.count { it.status == OrderStatus.PENDING }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Pedidos", fontWeight = FontWeight.Bold)
                            if (pendingCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFF57F17),
                                    contentColor = Color.White,
                                ) {
                                    Text("$pendingCount novos", fontSize = 11.sp)
                                }
                            }
                        }
                    },
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
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // ── Filtros ──────────────────────────────────────────────────
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(OrderFilter.entries) { filter ->
                        val count = allOrders.count { it.matchesFilter(filter) }
                            .takeIf { filter != OrderFilter.ALL }
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    if (count != null) "${filter.label} ($count)"
                                    else filter.label,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }

                // ── Lista ────────────────────────────────────────────────────
                if (filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("📭", fontSize = 48.sp)
                            Text(
                                "Nenhum pedido aqui",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                onStatusChange = screenModel::updateOrderStatus,
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ─── Card do pedido ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderCard(
    order: Order,
    onStatusChange: (String, OrderStatus) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(order.status in ACTIVE_STATUSES) }

    val statusColor by animateColorAsState(
        targetValue = order.status.statusColor(),
        animationSpec = tween(400),
        label = "statusColor",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Barra lateral colorida por status
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(statusColor),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
            ) {
                // ── Cabeçalho ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Avatar inicial + nome
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = order.customerName.firstOrNull()?.uppercase() ?: "?",
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontSize = 16.sp,
                            )
                        }
                        Column {
                            Text(
                                text = order.customerName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = orderTimestamp(order.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Status badge clicável (dropdown)
                    ExposedDropdownMenuBox(
                        expanded = statusMenuExpanded,
                        onExpandedChange = { statusMenuExpanded = it },
                    ) {
                        StatusPill(
                            label = order.status.label(),
                            color = statusColor,
                            modifier = Modifier.menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false },
                        ) {
                            OrderStatus.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(status.statusColor()),
                                            )
                                            Text(status.label())
                                        }
                                    },
                                    onClick = {
                                        onStatusChange(order.id, status)
                                        statusMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                // ── Resumo dos itens (sempre visível) ─────────────────────
                if (order.items.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = order.items.joinToString("  ·  ") { "${it.quantity}× ${it.productName}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Total sempre visível
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "R$ ${order.total.formatPrice()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor,
                )

                // ── Detalhes expansíveis ───────────────────────────────────
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )

                        // Itens detalhados
                        if (order.items.size > 1) {
                            order.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "${item.quantity}× ${item.productName}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        text = "R$ ${item.subtotal.formatPrice()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Telefone
                        if (order.customerPhone.isNotEmpty()) {
                            InfoRow(icon = "📞", text = order.customerPhone)
                        }

                        // Endereço
                        if (order.customerAddress.isNotEmpty()) {
                            InfoRow(icon = "📍", text = order.customerAddress)
                        }

                        // Observações
                        if (order.notes.isNotEmpty()) {
                            InfoRow(
                                icon = "💬",
                                text = order.notes,
                                textColor = MaterialTheme.colorScheme.tertiary,
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )

                        // ── Botões de ação ─────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Próximo status (ação primária)
                            val nextStatus = order.status.next()
                            if (nextStatus != null) {
                                Button(
                                    onClick = { onStatusChange(order.id, nextStatus) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = statusColor,
                                    ),
                                ) {
                                    Text(
                                        text = order.status.nextLabel(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            // WhatsApp
                            if (order.customerPhone.isNotEmpty()) {
                                val waNumber = formatWhatsAppNumber(order.customerPhone)
                                val waMessage = "Olá ${order.customerName}! Seu pedido está: ${order.status.label()}."
                                val waUrl = "https://wa.me/$waNumber?text=${waMessage.encodeUrl()}"

                                Button(
                                    onClick = {
                                        try { uriHandler.openUri(waUrl) } catch (_: Exception) {}
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF25D366),
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            // Cancelar (só para ativos)
                            if (order.status in ACTIVE_STATUSES) {
                                OutlinedButton(
                                    onClick = { onStatusChange(order.id, OrderStatus.CANCELLED) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text("✕", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Indicador de expandir
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = if (expanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// ─── Componentes auxiliares ───────────────────────────────────────────────────

@Composable
private fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: String,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(icon, fontSize = 13.sp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
        )
    }
}

// Encode simples para URL (sem dependência externa)
private fun String.encodeUrl(): String = this
    .replace(" ", "%20")
    .replace("!", "%21")
    .replace("\"", "%22")
    .replace("#", "%23")
    .replace("$", "%24")
    .replace("&", "%26")
    .replace("'", "%27")
    .replace("(", "%28")
    .replace(")", "%29")
    .replace("*", "%2A")
    .replace("+", "%2B")
    .replace(",", "%2C")
    .replace("/", "%2F")
    .replace(":", "%3A")
    .replace(";", "%3B")
    .replace("=", "%3D")
    .replace("?", "%3F")
    .replace("@", "%40")
    .replace("[", "%5B")
    .replace("]", "%5D")
