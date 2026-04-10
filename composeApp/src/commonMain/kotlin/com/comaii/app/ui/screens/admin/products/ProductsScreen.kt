package com.comaii.app.ui.screens.admin.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.comaii.app.ui.util.formatPrice
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.comaii.shared.domain.model.Product

data class ProductsScreen(val companyId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ProductsScreenModel> { parametersOf(companyId) }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Produtos") },
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
                    actions = {
                        TextButton(onClick = screenModel::showAddCategoryDialog) {
                            Text("+ Categoria", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { screenModel.showAddProductDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Text("+", style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            },
        ) { padding ->
            if (state.products.isEmpty() && !state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Nenhum produto cadastrado",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Toque em + para adicionar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val categoryMap = state.categories.associateBy { it.id }
                // Group by categoryId (String, never null). Unknown/empty = "Sem categoria"
                val grouped: Map<String, List<Product>> = state.products.groupBy { it.categoryId }
                // Show known categories in order, then "Sem categoria" at the end
                val orderedKeys: List<String> = state.categories
                    .map { it.id }
                    .filter { grouped.containsKey(it) } +
                    grouped.keys.filter { it.isEmpty() || !categoryMap.containsKey(it) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    orderedKeys.forEach { categoryId ->
                        val categoryName = categoryMap[categoryId]?.name
                            ?: "Sem categoria"
                        val products = grouped[categoryId] ?: emptyList()

                        item(key = "header_$categoryId") {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }

                        items(products, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                categoryName = null,
                                onEdit = { screenModel.showAddProductDialog(product) },
                                onDelete = { screenModel.deleteProduct(product.id) },
                            )
                        }
                    }
                }
            }
        }

        // Dialog adicionar/editar produto
        if (state.showAddDialog) {
            AddProductDialog(
                state = state,
                onNameChange = screenModel::onFormNameChange,
                onDescriptionChange = screenModel::onFormDescriptionChange,
                onPriceChange = screenModel::onFormPriceChange,
                onCostChange = screenModel::onFormCostChange,
                onImageUrlChange = screenModel::onFormImageUrlChange,
                onCategoryChange = screenModel::onFormCategoryIdChange,
                onSave = screenModel::saveProduct,
                onDismiss = screenModel::hideAddProductDialog,
            )
        }

        // Dialog adicionar categoria
        if (state.showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = screenModel::hideAddCategoryDialog,
                title = { Text("Nova categoria") },
                text = {
                    OutlinedTextField(
                        value = state.formCategoryName,
                        onValueChange = screenModel::onFormCategoryNameChange,
                        label = { Text("Nome da categoria") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    Button(onClick = screenModel::saveCategory) { Text("Salvar") }
                },
                dismissButton = {
                    TextButton(onClick = screenModel::hideAddCategoryDialog) { Text("Cancelar") }
                },
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    categoryName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (product.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (product.description.isNotEmpty()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (categoryName != null) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "R$ ${product.price.formatPrice()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    state: ProductsUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (state.editingProduct != null) "Editar produto" else "Novo produto")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.formName,
                    onValueChange = onNameChange,
                    label = { Text("Nome do produto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.formDescription,
                    onValueChange = onDescriptionChange,
                    label = { Text("Descrição (opcional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.formPrice,
                    onValueChange = onPriceChange,
                    label = { Text("Preço (R$)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.formCost,
                    onValueChange = onCostChange,
                    label = { Text("Custo de fabricação (R$)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.formImageUrl,
                    onValueChange = onImageUrlChange,
                    label = { Text("URL da imagem (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.formImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = state.formImageUrl,
                        contentDescription = "Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }

                // Seletor de categoria
                if (state.categories.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = state.categories.find { it.id == state.formCategoryId }?.name ?: "Sem categoria",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoria") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sem categoria") },
                                onClick = {
                                    onCategoryChange("")
                                    categoryExpanded = false
                                },
                            )
                            state.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        onCategoryChange(category.id)
                                        categoryExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                if (state.error != null) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
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
