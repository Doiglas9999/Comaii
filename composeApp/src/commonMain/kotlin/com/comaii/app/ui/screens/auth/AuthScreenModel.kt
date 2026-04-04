package com.comaii.app.ui.screens.auth

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.comaii.shared.data.firebase.FirebaseService
import com.comaii.shared.domain.model.Company
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val companyName: String = "",
    val isLogin: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val companyId: String? = null,
)

class AuthScreenModel(
    private val firebase: FirebaseService,
) : ScreenModel {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        if (firebase.isLoggedIn) {
            _state.value = _state.value.copy(
                isAuthenticated = true,
                companyId = firebase.currentUserId,
            )
        }
    }

    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun onCompanyNameChange(name: String) {
        _state.value = _state.value.copy(companyName = name, error = null)
    }

    fun toggleMode() {
        _state.value = _state.value.copy(isLogin = !_state.value.isLogin, error = null)
    }

    fun submit() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Preencha todos os campos")
            return
        }
        if (!s.isLogin && s.companyName.isBlank()) {
            _state.value = s.copy(error = "Informe o nome da empresa")
            return
        }

        _state.value = s.copy(isLoading = true, error = null)
        screenModelScope.launch {
            if (s.isLogin) login() else register()
        }
    }

    private suspend fun login() {
        val s = _state.value
        firebase.signInWithEmail(s.email, s.password)
            .onSuccess { userId ->
                _state.value = s.copy(isLoading = false, isAuthenticated = true, companyId = userId)
            }
            .onFailure { e ->
                _state.value = s.copy(isLoading = false, error = "Erro ao entrar: ${e.message}")
            }
    }

    private suspend fun register() {
        val s = _state.value
        firebase.signUpWithEmail(s.email, s.password)
            .onSuccess { userId ->
                val company = Company(
                    id = userId,
                    name = s.companyName,
                    slug = s.companyName.lowercase()
                        .replace(Regex("[^a-z0-9]"), "-")
                        .replace(Regex("-+"), "-")
                        .trim('-'),
                    ownerId = userId,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                )
                firebase.saveCompany(company)
                _state.value = s.copy(isLoading = false, isAuthenticated = true, companyId = userId)
            }
            .onFailure { e ->
                _state.value = s.copy(isLoading = false, error = "Erro ao cadastrar: ${e.message}")
            }
    }

    fun signOut() {
        screenModelScope.launch {
            firebase.signOut()
            _state.value = AuthUiState()
        }
    }
}
