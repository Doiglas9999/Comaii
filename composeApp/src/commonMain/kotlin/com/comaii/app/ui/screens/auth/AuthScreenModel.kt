package com.comaii.app.ui.screens.auth

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.comaii.app.CredentialStorage
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
    val isOwnerMode: Boolean = true, // true = empresa, false = cliente
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val companyId: String? = null,
    val rememberCredentials: Boolean = false,
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
        } else {
            // Carregar credenciais salvas
            val saved = CredentialStorage.load()
            if (saved != null) {
                _state.value = _state.value.copy(
                    email = saved.first,
                    password = saved.second,
                    rememberCredentials = true,
                )
                // Auto-login com credenciais salvas
                screenModelScope.launch { autoLogin(saved.first, saved.second) }
            }
        }
    }

    private suspend fun autoLogin(email: String, password: String) {
        _state.value = _state.value.copy(isLoading = true)
        firebase.signInWithEmail(email, password)
            .onSuccess { userId ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    companyId = userId,
                )
            }
            .onFailure {
                // Credenciais inválidas — limpar e deixar o usuário logar manualmente
                CredentialStorage.clear()
                _state.value = _state.value.copy(
                    isLoading = false,
                    password = "",
                    rememberCredentials = false,
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

    fun onRememberChange(value: Boolean) {
        _state.value = _state.value.copy(rememberCredentials = value)
    }

    fun toggleMode() {
        _state.value = _state.value.copy(isLogin = !_state.value.isLogin, error = null, companyName = "")
    }

    fun toggleOwnerMode() {
        _state.value = _state.value.copy(isOwnerMode = !_state.value.isOwnerMode, error = null, companyName = "")
    }

    fun submit() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Preencha todos os campos")
            return
        }
        if (!s.isLogin && s.isOwnerMode && s.companyName.isBlank()) {
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
                if (s.rememberCredentials) {
                    CredentialStorage.save(s.email, s.password)
                } else {
                    CredentialStorage.clear()
                }
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
                if (s.isOwnerMode) {
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
                } else {
                    // Customer: just auth, no company
                    _state.value = s.copy(isLoading = false, isAuthenticated = true, companyId = null)
                }
            }
            .onFailure { e ->
                _state.value = s.copy(isLoading = false, error = "Erro ao cadastrar: ${e.message}")
            }
    }

    fun signOut() {
        screenModelScope.launch {
            firebase.signOut()
            CredentialStorage.clear()
            _state.value = AuthUiState()
        }
    }
}
