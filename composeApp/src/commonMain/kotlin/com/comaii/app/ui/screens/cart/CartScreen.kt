package com.comaii.app.ui.screens.cart

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.comaii.app.ui.screens.customer.CustomerOrdersScreen
import com.comaii.app.ui.screens.store.StoreScreenModel
import com.comaii.app.ui.util.formatPrice
import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.domain.model.Order
import com.comaii.shared.domain.model.OrderItem
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

data class CartScreen(val companyId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val storeModel = navigator.koinNavigatorScreenModel<StoreScreenModel> { parametersOf(companyId) }
        val state by storeModel.state.collectAsState()
        val firebase = koinInject<FirebaseService>()
        val scope = rememberCoroutineScope()

        // Pre-fill from registration data if available
        var customerName by remember { mutableStateOf(state.authName) }
        var customerPhone by remember { mutableStateOf(state.authPhone) }
        var customerAddress by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }
        var orderPlaced by remember { mutableStateOf(false) }
        var placedOrderId by remember { mutableStateOf("") }

        val realCompanyId = state.company?.id ?: companyId
        val canSubmit = customerName.isNotBlank() &&
            customerPhone.isNotBlank() &&
            state.cartItems.isNotEmpty() &&
            !isSubmitting

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (orderPlaced) "Pedido realizado!" else "Meu carrinho",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = { navigator.pop() }) {
                            Text("← Voltar", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
            bottomBar = {
                if (!orderPlaced) {
                    Surface(
                        shadowElevation = 12.dp,
                        tonalElevation = 4.dp,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Total do pedido",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "R$ ${state.cartTotal.formatPrice()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (canSubmit) {
                                        isSubmitting = true
                                        scope.launch {
                                            val orderId = firebase.createOrder(
                                                realCompanyId,
                                                Order(
                                                    companyId = realCompanyId,
                                                    customerId = state.currentUserId ?: "",
                                                    customerName = customerName,
                                                    customerPhone = customerPhone,
                                                    customerAddress = customerAddress,
                                                    notes = notes,
                                                    items = state.cartItems.map { (product, qty) ->
                                                        OrderItem(
                                                            productId = product.id,
                                                            productName = product.name,
                                                            quantity = qty,
                                                            unitPrice = product.price,
                                                        )
                                                    },
                                                    total = state.cartTotal,
                                                ),
                                            )
                                            placedOrderId = orderId
                                            storeModel.clearCart()
                                            isSubmitting = false
                                            orderPlaced = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                enabled = canSubmit,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    text = if (isSubmitting) "Enviando..." else "Confirmar pedido",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            if (orderPlaced) {
                // Order success screen
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("✅", style = MaterialTheme.typography.displayLarge)
                        Text(
                            text = "Pedido enviado!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Aguarde a confirmação da loja.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val userId = state.currentUserId
                                if (userId != null) {
                                    navigator.push(
                                        CustomerOrdersScreen(
                                            companyId = realCompanyId,
                                            customerId = userId,
                                        ),
                                    )
                                } else {
                                    navigator.popUntilRoot()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("📦 Acompanhar pedido")
                        }
                        TextButton(
                            onClick = { navigator.popUntilRoot() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Voltar à loja")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Cart items header
                    item {
                        Text(
                            text = "Itens do pedido",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Cart items with quantity controls
                    items(state.cartItems, key = { it.first.id }) { (product, qty) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(1.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
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
                                        text = product.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "R$ ${product.price.formatPrice()} cada",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                // Quantity controls
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = { storeModel.removeFromCart(product) },
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                    ) {
                                        Text("−", fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "$qty",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp),
                                    )
                                    Button(
                                        onClick = { storeModel.addToCart(product) },
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(0.dp),
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Subtotal
                                Text(
                                    text = "R$ ${(product.price * qty).formatPrice()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    // Divider
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Customer info section
                    item {
                        Text(
                            text = "Dados de entrega",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Seu nome *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Telefone (WhatsApp) *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = customerAddress,
                            onValueChange = { customerAddress = it },
                            label = { Text("Endereço de entrega") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Observações (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                    }

                    // Bottom spacing for the fixed bottom bar
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
