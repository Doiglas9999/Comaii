package com.comaii.app

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.comaii.app.di.allModules
import com.comaii.app.ui.screens.auth.AuthScreen
import com.comaii.app.ui.screens.store.StoreScreen
import com.comaii.app.ui.theme.ComaiiTheme
import org.koin.compose.KoinApplication

@Composable
fun App(companyId: String = "default") {
    KoinApplication(application = {
        modules(allModules)
    }) {
        ComaiiTheme {
            // Se o companyId for "default" ou "admin", mostra tela de login do admin
            // Se for um ID real, mostra a loja da empresa
            val startScreen = if (companyId == "default" || companyId == "admin") {
                AuthScreen()
            } else {
                StoreScreen(companyId)
            }

            Navigator(startScreen)
        }
    }
}
