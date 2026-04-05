package com.comaii.app.ui.screens.admin.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.domain.model.Company
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoreSettingsUiState(
    val name: String = "",
    val description: String = "",
    val slug: String = "",
    val phone: String = "",
    val address: String = "",
    val primaryColor: String = "#FF6B00",
    val secondaryColor: String = "#FFFFFF",
    val accentColor: String = "#333333",
    val isOpen: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
)

class StoreSettingsScreenModel(
    private val companyId: String,
    private val firebase: FirebaseService,
) : ScreenModel {

    private val _state = MutableStateFlow(StoreSettingsUiState())
    val state: StateFlow<StoreSettingsUiState> = _state.asStateFlow()

    private var currentCompany: Company? = null

    init {
        screenModelScope.launch {
            firebase.observeCompany(companyId).collect { company ->
                if (company != null && currentCompany == null) {
                    currentCompany = company
                    _state.value = _state.value.copy(
                        name = company.name,
                        description = company.description,
                        slug = company.slug,
                        phone = company.phone,
                        address = company.address,
                        primaryColor = company.primaryColor,
                        secondaryColor = company.secondaryColor,
                        accentColor = company.accentColor,
                        isOpen = company.isOpen,
                        isLoading = false,
                    )
                } else if (company != null) {
                    currentCompany = company
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value, error = null, saveSuccess = false)
    }

    fun onDescriptionChange(value: String) {
        _state.value = _state.value.copy(description = value, error = null, saveSuccess = false)
    }

    fun onPhoneChange(value: String) {
        _state.value = _state.value.copy(phone = value, error = null, saveSuccess = false)
    }

    fun onAddressChange(value: String) {
        _state.value = _state.value.copy(address = value, error = null, saveSuccess = false)
    }

    fun onPrimaryColorChange(value: String) {
        _state.value = _state.value.copy(primaryColor = value, error = null, saveSuccess = false)
    }

    fun onSecondaryColorChange(value: String) {
        _state.value = _state.value.copy(secondaryColor = value, error = null, saveSuccess = false)
    }

    fun onAccentColorChange(value: String) {
        _state.value = _state.value.copy(accentColor = value, error = null, saveSuccess = false)
    }

    fun toggleOpen() {
        _state.value = _state.value.copy(isOpen = !_state.value.isOpen, saveSuccess = false)
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Nome da loja e obrigatorio")
            return
        }

        val company = currentCompany ?: return

        _state.value = s.copy(isSaving = true, error = null, saveSuccess = false)

        screenModelScope.launch {
            try {
                firebase.saveCompany(
                    company.copy(
                        name = s.name,
                        description = s.description,
                        phone = s.phone,
                        address = s.address,
                        primaryColor = s.primaryColor,
                        secondaryColor = s.secondaryColor,
                        accentColor = s.accentColor,
                        isOpen = s.isOpen,
                    )
                )
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = "Erro ao salvar: ${e.message}",
                )
            }
        }
    }
}
