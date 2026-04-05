package com.comaii.app.ui.screens.admin.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf

data class StoreSettingsScreen(val companyId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<StoreSettingsScreenModel> { parametersOf(companyId) }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Configurar Loja") },
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
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Store link
                if (state.slug.isNotEmpty()) {
                    Text(
                        text = "Link da loja: comaii.com/${state.slug}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Name
                OutlinedTextField(
                    value = state.name,
                    onValueChange = screenModel::onNameChange,
                    label = { Text("Nome da loja") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Description
                OutlinedTextField(
                    value = state.description,
                    onValueChange = screenModel::onDescriptionChange,
                    label = { Text("Descricao") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Phone
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = screenModel::onPhoneChange,
                    label = { Text("Telefone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Address
                OutlinedTextField(
                    value = state.address,
                    onValueChange = screenModel::onAddressChange,
                    label = { Text("Endereco") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Cores",
                    style = MaterialTheme.typography.titleMedium,
                )

                // Primary color
                ColorPickerField(
                    label = "Cor primaria",
                    value = state.primaryColor,
                    onValueChange = screenModel::onPrimaryColorChange,
                )

                // Secondary color
                ColorPickerField(
                    label = "Cor secundaria",
                    value = state.secondaryColor,
                    onValueChange = screenModel::onSecondaryColorChange,
                )

                // Accent color
                ColorPickerField(
                    label = "Cor de destaque",
                    value = state.accentColor,
                    onValueChange = screenModel::onAccentColorChange,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // isOpen switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Loja aberta",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Switch(
                        checked = state.isOpen,
                        onCheckedChange = { screenModel.toggleOpen() },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Error
                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // Success
                if (state.saveSuccess) {
                    Text(
                        text = "Configuracoes salvas com sucesso!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Save button
                Button(
                    onClick = screenModel::save,
                    enabled = !state.isSaving && !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isSaving) "Salvando..." else "Salvar")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ColorPickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .background(parseHexColor(value)),
        )
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        when (cleaned.length) {
            6 -> Color(
                red = cleaned.substring(0, 2).toInt(16) / 255f,
                green = cleaned.substring(2, 4).toInt(16) / 255f,
                blue = cleaned.substring(4, 6).toInt(16) / 255f,
            )
            8 -> Color(
                alpha = cleaned.substring(0, 2).toInt(16) / 255f,
                red = cleaned.substring(2, 4).toInt(16) / 255f,
                green = cleaned.substring(4, 6).toInt(16) / 255f,
                blue = cleaned.substring(6, 8).toInt(16) / 255f,
            )
            else -> Color.Gray
        }
    } catch (_: Exception) {
        Color.Gray
    }
}
