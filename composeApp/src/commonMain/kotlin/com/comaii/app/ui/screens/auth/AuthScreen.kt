package com.comaii.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.comaii.app.ui.screens.admin.AdminDashboardScreen

class AuthScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<AuthScreenModel>()
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        // Navegar quando autenticado como empresa
        LaunchedEffect(state.isAuthenticated, state.companyId) {
            if (state.isAuthenticated && state.companyId != null) {
                navigator.replaceAll(AdminDashboardScreen(state.companyId!!))
            }
        }

        // Tela de sucesso para cliente
        if (state.isAuthenticated && state.companyId == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Conta criada!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Para fazer seu pedido, acesse o link da loja compartilhado com você.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo / Título
            Text(
                text = "Comaii",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Seu cardápio digital",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Título do formulário
            Text(
                text = if (state.isLogin) "Entrar" else "Criar conta",
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Seletor de tipo de conta (só no registro)
            AnimatedVisibility(visible = !state.isLogin) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { if (!state.isOwnerMode) screenModel.toggleOwnerMode() },
                            modifier = Modifier.weight(1f),
                            colors = if (state.isOwnerMode)
                                ButtonDefaults.buttonColors()
                            else
                                ButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text("Tenho uma loja")
                        }
                        OutlinedButton(
                            onClick = { if (state.isOwnerMode) screenModel.toggleOwnerMode() },
                            modifier = Modifier.weight(1f),
                            colors = if (!state.isOwnerMode)
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                )
                            else
                                ButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text("Sou cliente")
                        }
                    }
                    if (state.isOwnerMode) {
                        OutlinedTextField(
                            value = state.companyName,
                            onValueChange = screenModel::onCompanyNameChange,
                            label = { Text("Nome da empresa") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )
                    }
                }
            }

            if (!state.isLogin) Spacer(modifier = Modifier.height(8.dp))

            // Email
            OutlinedTextField(
                value = state.email,
                onValueChange = screenModel::onEmailChange,
                label = { Text("E-mail") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Senha
            OutlinedTextField(
                value = state.password,
                onValueChange = screenModel::onPasswordChange,
                label = { Text("Senha") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lembrar credenciais
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.rememberCredentials,
                    onCheckedChange = screenModel::onRememberChange,
                )
                Text(
                    text = "Lembrar e-mail e senha",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // Erro
            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botão principal
            Button(
                onClick = screenModel::submit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(if (state.isLogin) "Entrar" else "Criar conta")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Alternar login/registro
            TextButton(onClick = screenModel::toggleMode) {
                Text(
                    if (state.isLogin)
                        "Não tem conta? Cadastre-se"
                    else
                        "Já tem conta? Faça login"
                )
            }
        }
    }
}
