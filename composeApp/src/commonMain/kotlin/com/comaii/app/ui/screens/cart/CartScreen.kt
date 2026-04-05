package com.comaii.app.ui.screens.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf
import com.comaii.app.ui.screens.store.StoreScreenModel
import com.comaii.app.ui.util.formatPrice
import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.domain.model.Order
import com.comaii.shared.domain.model.OrderItem
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class CartScreen(val companyId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val storeModel = koinScreenModel<StoreScreenModel> { parametersOf(companyId) }
        val state by storeModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val firebase = koinInject<FirebaseService>()
        val scope = rememberCoroutineScope()

        var customerName by remember { mutableStateOf("") }
        var customerPhone by remember { mutableStateOf("") }
        var customerAddress by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var orderPlaced by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Carrinho") },
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
            if (orderPlaced) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Pedido enviado!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Aguarde a confirmação do estabelecimento.")
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navigator.popUntilRoot() }) {
                        Text("Voltar à loja")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Itens do carrinho
                    items(state.cartItems) { (product, qty) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(1.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    Text("${qty}x R$ ${product.price.formatPrice()}")
                                }
                                Text(
                                    "R$ ${(product.price * qty).formatPrice()}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    // Total
                    item {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "R$ ${state.cartTotal.formatPrice()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    // Dados do cliente
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Seus dados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Nome") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Telefone") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = customerAddress,
                            onValueChange = { customerAddress = it },
                            label = { Text("Endereço") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Observações (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Botão finalizar
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (customerName.isNotBlank() && customerPhone.isNotBlank()) {
                                    scope.launch {
                                        firebase.createOrder(
                                            companyId,
                                            Order(
                                                companyId = companyId,
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
                                            )
                                        )
                                        orderPlaced = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = customerName.isNotBlank() && customerPhone.isNotBlank()
                                    && state.cartItems.isNotEmpty(),
                        ) {
                            Text("Finalizar pedido")
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
