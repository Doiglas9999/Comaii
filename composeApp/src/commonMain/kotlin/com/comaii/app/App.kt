package com.comaii.app

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.comaii.app.ui.screens.home.HomeScreen
import com.comaii.app.ui.theme.ComaiiTheme

@Composable
fun App(companyId: String = "default") {
    ComaiiTheme {
        Navigator(HomeScreen(companyId))
    }
}
