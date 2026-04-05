package com.comaii.app.ui.screens.admin.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.comaii.shared.domain.model.DaySchedule
import org.koin.core.parameter.parametersOf

private val PRESET_COLORS = listOf(
    "#FF6B00", "#E53935", "#8E24AA", "#1E88E5",
    "#00ACC1", "#43A047", "#FFB300", "#6D4C41",
    "#546E7A", "#000000", "#FFFFFF", "#F5F5F5",
    "#FF8A65", "#A5D6A7", "#90CAF9", "#CE93D8",
)

private val DAY_KEYS = listOf(
    "monday" to "Segunda",
    "tuesday" to "Terça",
    "wednesday" to "Quarta",
    "thursday" to "Quinta",
    "friday" to "Sexta",
    "saturday" to "Sábado",
    "sunday" to "Domingo",
)

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
                    currentHex = state.primaryColor,
                    onColorSelected = screenModel::onPrimaryColorChange,
                )

                // Secondary color
                ColorPickerField(
                    label = "Cor secundaria",
                    currentHex = state.secondaryColor,
                    onColorSelected = screenModel::onSecondaryColorChange,
                )

                // Accent color
                ColorPickerField(
                    label = "Cor de destaque",
                    currentHex = state.accentColor,
                    onColorSelected = screenModel::onAccentColorChange,
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

                // Working days section
                Text(
                    text = "Horário de Funcionamento",
                    style = MaterialTheme.typography.titleMedium,
                )

                val wd = state.workingDays
                val daySchedules = listOf(
                    "monday" to wd.monday,
                    "tuesday" to wd.tuesday,
                    "wednesday" to wd.wednesday,
                    "thursday" to wd.thursday,
                    "friday" to wd.friday,
                    "saturday" to wd.saturday,
                    "sunday" to wd.sunday,
                )

                DAY_KEYS.forEachIndexed { index, (dayKey, dayLabel) ->
                    val schedule = daySchedules[index].second
                    WorkingDayRow(
                        dayLabel = dayLabel,
                        schedule = schedule,
                        onToggle = { isOpen -> screenModel.onDayToggle(dayKey, isOpen) },
                        onOpenTimeChange = { time -> screenModel.onDayOpenTimeChange(dayKey, time) },
                        onCloseTimeChange = { time -> screenModel.onDayCloseTimeChange(dayKey, time) },
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
    currentHex: String,
    onColorSelected: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var customHex by remember(currentHex) { mutableStateOf(currentHex) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .background(parseHexColor(currentHex))
                .clickable { showDialog = true },
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Preset color grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    ) {
                        items(PRESET_COLORS) { hex ->
                            val isSelected = hex.equals(currentHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(parseHexColor(hex))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(6.dp),
                                    )
                                    .clickable {
                                        customHex = hex
                                        onColorSelected(hex)
                                        showDialog = false
                                    },
                            )
                        }
                    }

                    // Custom hex input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = customHex,
                            onValueChange = { customHex = it },
                            label = { Text("Hex personalizado") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("#RRGGBB") },
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                .background(parseHexColor(customHex)),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onColorSelected(customHex)
                        showDialog = false
                    },
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
fun WorkingDayRow(
    dayLabel: String,
    schedule: DaySchedule,
    onToggle: (Boolean) -> Unit,
    onOpenTimeChange: (String) -> Unit,
    onCloseTimeChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(80.dp),
            )
            Switch(
                checked = schedule.isOpen,
                onCheckedChange = onToggle,
            )
            if (schedule.isOpen) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedTextField(
                        value = schedule.openTime,
                        onValueChange = onOpenTimeChange,
                        label = { Text("Abre") },
                        placeholder = { Text("HH:MM") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp),
                    )
                    Text(
                        text = "–",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = schedule.closeTime,
                        onValueChange = onCloseTimeChange,
                        label = { Text("Fecha") },
                        placeholder = { Text("HH:MM") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp),
                    )
                }
            } else {
                Text(
                    text = "Fechado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
