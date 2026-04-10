package com.comaii.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.comaii.app.getStoreBaseUrl
import com.comaii.app.ui.screens.admin.categories.CategoriesScreen
import com.comaii.app.ui.screens.admin.orders.OrdersDashboardScreen
import com.comaii.app.ui.screens.admin.orders.OrdersListScreen
import com.comaii.app.ui.screens.admin.products.ProductsScreen
import com.comaii.app.ui.screens.admin.settings.StoreSettingsScreen
import com.comaii.app.ui.screens.auth.AuthScreen
import com.comaii.app.ui.screens.auth.AuthScreenModel
import org.koin.core.parameter.parametersOf

data class AdminDashboardScreen(val companyId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val authScreenModel = koinScreenModel<AuthScreenModel>()
        val dashboardModel = koinScreenModel<AdminDashboardScreenModel> { parametersOf(companyId) }
        val state by dashboardModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.company?.name ?: "Comaii Admin",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    actions = {
                        IconButton(onClick = {
                            authScreenModel.signOut()
                            navigator.replaceAll(AuthScreen())
                        }) {
                            Text("Sair", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Info da empresa
                if (state.company != null) {
                    Text(
                        text = "Painel de controle",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "${getStoreBaseUrl()}/${state.company!!.slug}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Linha 1: Produtos | Pedidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DashboardCard(
                        title = "Produtos",
                        count = "${state.productCount}",
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(ProductsScreen(companyId)) },
                    )
                    DashboardCard(
                        title = "Pedidos",
                        count = if (state.pendingOrderCount > 0) "${state.pendingOrderCount} novos" else "${state.orderCount}",
                        highlight = state.pendingOrderCount > 0,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(OrdersListScreen(companyId)) },
                    )
                }

                // Linha 2: Categorias | Dashboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DashboardCard(
                        title = "Categorias",
                        count = "${state.categoryCount}",
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(CategoriesScreen(companyId)) },
                    )
                    DashboardCard(
                        title = "Dashboard",
                        count = "",
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(OrdersDashboardScreen(companyId)) },
                    )
                }

                // Linha 3: Configurar Loja (largura total)
                DashboardCard(
                    title = "Configurar Loja",
                    count = "",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navigator.push(StoreSettingsScreen(companyId)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCard(
    title: String,
    count: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: () -> Unit = {},
) {
    val containerColor = if (highlight)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.primaryContainer

    val contentColor = if (highlight)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    val countColor = if (highlight)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.primary

    Card(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (count.isNotEmpty()) {
                Text(
                    text = count,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = countColor,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
        }
    }
}
